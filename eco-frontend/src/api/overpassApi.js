// Server-side proxy first (avoids CORS), then direct mirrors as fallback
const ENDPOINTS = [
  '/overpass',
  '/overpass2',
  'https://overpass.kumi.systems/api/interpreter',
  'https://overpass-api.de/api/interpreter',
]

// Dutch chains legally required to accept statiegeld
const DUTCH_CHAINS = [
  'albert heijn', 'ah to go', 'jumbo', 'lidl', 'aldi', 'dirk',
  'plus supermarkt', 'spar', 'coop', 'vomar', 'dekamarkt', 'deka',
  'poiesz', 'boni', 'jan linders', 'hoogvliet', 'picnic',
]

// In-memory cache to avoid duplicate API requests
const cache = new Map()

function tagLabel(tags) {
  return tags.name || tags.brand || tags.operator || 'Supermarkt'
}

function tagAddress(tags) {
  return [tags['addr:street'], tags['addr:housenumber'], tags['addr:city']]
    .filter(Boolean).join(' ')
}

function tagChain(tags) {
  const raw = (tags.brand || tags.name || tags.operator || '').toLowerCase()
  if (raw.includes('albert heijn') || /\bah\b/.test(raw)) return 'AH'
  if (raw.includes('jumbo'))     return 'Jumbo'
  if (raw.includes('lidl'))      return 'Lidl'
  if (raw.includes('aldi'))      return 'Aldi'
  if (raw.includes('dirk'))      return 'Dirk'
  if (raw.includes('plus'))      return 'Plus'
  if (raw.includes('spar'))      return 'Spar'
  if (raw.includes('coop'))      return 'Coop'
  if (raw.includes('vomar'))     return 'Vomar'
  if (raw.includes('deka'))      return 'Deka'
  if (raw.includes('poiesz'))    return 'Poiesz'
  if (raw.includes('hoogvliet')) return 'Hoogvliet'
  if (raw.includes('boni'))      return 'Boni'
  return null
}

function parseOpenUntil(oh) {
  try {
    const days  = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']
    const today = days[new Date().getDay()]
    for (const part of oh.split(';')) {
      const m = part.trim().match(/^([A-Za-z,\-]+)\s+\d{2}:\d{2}-(\d{2}:\d{2})/)
      if (!m) continue
      const range = expandRange(m[1])
      if (range.includes(today)) return m[2]
    }
    const fb = oh.match(/\d{2}:\d{2}-(\d{2}:\d{2})/)
    return fb ? fb[1] : null
  } catch { return null }
}

function expandRange(spec) {
  const order = ['Mo','Tu','We','Th','Fr','Sa','Su']
  const m = spec.match(/^([A-Z][a-z])-([A-Z][a-z])$/)
  if (m) {
    const s = order.indexOf(m[1]), e = order.indexOf(m[2])
    if (s >= 0 && e >= 0) return order.slice(s, e + 1)
  }
  return spec.split(',').map(s => s.trim())
}

function haversine(lat1, lng1, lat2, lng2) {
  const R = 6371000, rad = Math.PI / 180
  const dLat = (lat2 - lat1) * rad, dLng = (lng2 - lng1) * rad
  const a = Math.sin(dLat/2)**2 + Math.cos(lat1*rad)*Math.cos(lat2*rad)*Math.sin(dLng/2)**2
  return Math.round(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)))
}

function toStore(el) {
  const tags = el.tags || {}
  const lat  = el.type === 'node' ? el.lat : el.center?.lat
  const lng  = el.type === 'node' ? el.lon : el.center?.lon
  if (!lat || !lng) return null
  return {
    id:                `osm-${el.type}-${el.id}`,
    name:              tagLabel(tags),
    chain:             tagChain(tags),
    address:           tagAddress(tags),
    lat, lng,
    openUntil:         tags.opening_hours ? parseOpenUntil(tags.opening_hours) : null,
    acceptedMaterials: ['01 PET', 'Alu'],
    machineCount:      1,
    distanceMeters:    null,
  }
}

async function tryFetch(endpoint, query) {
  const url = `${endpoint}?data=${encodeURIComponent(query)}`
  const res  = await fetch(url, { signal: AbortSignal.timeout(15000) })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const json = await res.json()
  if (!json.elements) throw new Error('No elements in response')
  return json.elements
}

export async function fetchBottleReturnStores(lat, lng, radiusMeters = 5000) {
  // Rounded grid cache key (~1km precision)
  const cacheKey = `${lat.toFixed(2)}_${lng.toFixed(2)}_${radiusMeters}`
  if (cache.has(cacheKey)) {
    return cache.get(cacheKey)
  }

  const chainsRegex = 'Albert Heijn|Jumbo|Lidl|Aldi|Dirk|Plus|Spar|Coop|Vomar|Deka|Poiesz|Hoogvliet'

  // Server-side filtering in Overpass reduces payload size and query execution time
  const query = `[out:json][timeout:15];(node["shop"="supermarket"]["name"~"${chainsRegex}",i](around:${radiusMeters},${lat},${lng});way["shop"="supermarket"]["name"~"${chainsRegex}",i](around:${radiusMeters},${lat},${lng});node["vending"="bottle_return"](around:${radiusMeters},${lat},${lng}););out center tags;`

  let lastErr
  for (const endpoint of ENDPOINTS) {
    try {
      const elements = await tryFetch(endpoint, query)

      const stores = elements
        .map(toStore)
        .filter(Boolean)
        .map(s => ({ ...s, distanceMeters: haversine(lat, lng, s.lat, s.lng) }))
        .sort((a, b) => a.distanceMeters - b.distanceMeters)

      cache.set(cacheKey, stores)
      return stores
    } catch (err) {
      console.warn(`[Overpass] ${endpoint} failed:`, err.message)
      lastErr = err
    }
  }
  throw lastErr
}