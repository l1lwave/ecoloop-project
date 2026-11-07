import { useEffect, useState, useCallback, useRef, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { MapContainer, TileLayer, Marker, Circle, useMap, useMapEvents } from 'react-leaflet'
import MarkerClusterGroup from 'react-leaflet-cluster'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import { fetchBottleReturnStores } from '../api/overpassApi'
import { getNearby } from '../api/locationApi'
import LegalAttribution from '../components/legal/LegalAttribution'
import LanguageSwitcher from '../components/layout/LanguageSwitcher'

// Fix Leaflet default icon paths broken by Vite bundling
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl:       new URL('leaflet/dist/images/marker-icon.png',    import.meta.url).href,
  shadowUrl:     new URL('leaflet/dist/images/marker-shadow.png',  import.meta.url).href,
})

const DEFAULT_CENTER = [52.3676, 4.9041]
const SEARCH_RADIUS  = 8000 // metres

// per-chain pin colors — real hex values, since SVG fill can't use Tailwind utility classes
const CHAIN_HEX = {
  AH:    '#2563eb',
  Jumbo: '#eab308',
  Lidl:  '#facc15',
  Aldi:  '#3b82f6',
  Dirk:  '#ef4444',
  Plus:  '#22c55e',
  Spar:  '#dc2626',
  Coop:  '#f97316',
  Vomar: '#a855f7',
  Deka:  '#14b8a6',
}
const DEFAULT_PIN_COLOR = '#16a34a'

function pinSvg(color, size) {
  return `<svg width="${size}" height="${size * 1.286}" viewBox="0 0 28 36" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 0C6.27 0 0 6.27 0 14c0 9.63 14 22 14 22S28 23.63 28 14C28 6.27 21.73 0 14 0z" fill="${color}"/>
    <circle cx="14" cy="14" r="5.5" fill="white"/>
  </svg>`
}

// cache divIcons per chain/selected-state — avoid recreating on every render
const iconCache = new Map()
function getStoreIcon(chain, isSelected) {
  const key = `${chain}-${isSelected}`
  if (iconCache.has(key)) return iconCache.get(key)
  const color = CHAIN_HEX[chain] ?? DEFAULT_PIN_COLOR
  const size  = isSelected ? 34 : 28
  const icon  = L.divIcon({
    className: '',
    html: pinSvg(color, size),
    iconSize:   [size, size * 1.286],
    iconAnchor: [size / 2, size * 1.286],
    popupAnchor:[0, -(size * 1.286)],
  })
  iconCache.set(key, icon)
  return icon
}

const userIcon = L.divIcon({
  className: '',
  html: `<div style="width:16px;height:16px;border-radius:50%;background:#3b82f6;border:3px solid white;box-shadow:0 0 0 2px rgba(59,130,246,0.4)"></div>`,
  iconSize:   [16, 16],
  iconAnchor: [8, 8],
})

// Nominatim geocoding — Netherlands only
async function geocodeNL(query) {
  const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5&countrycodes=nl&addressdetails=1`
  const res = await fetch(url, { headers: { 'Accept-Language': 'nl' } })
  return res.json()
}

function MapController({ center, zoom }) {
  const map = useMap()
  useEffect(() => {
    if (center) map.flyTo(center, zoom ?? map.getZoom(), { duration: 0.8 })
  }, [center, zoom, map])
  return null
}

// tapping the map background (not a marker) deselects the open store card
function MapClickHandler({ onMapClick }) {
  useMapEvents({ click: onMapClick })
  return null
}

function formatDistance(meters) {
  return meters < 1000 ? `${Math.round(meters)} m` : `${(meters / 1000).toFixed(1)} km`
}

export default function MapPage() {
  const { t } = useTranslation()

  const [locations,  setLocations]  = useState([])
  const [selected,   setSelected]   = useState(null)
  const [loading,    setLoading]    = useState(false)
  const [loadError,  setLoadError]  = useState('')

  // map center for store loading (geolocation or searched place)
  const [searchCenter, setSearchCenter] = useState(null) // [lat, lng]
  const [userPos,      setUserPos]      = useState(null) // [lat, lng] — only real GPS
  const [mapTarget,    setMapTarget]    = useState(null) // { center, zoom }

  const [geoStatus, setGeoStatus] = useState('idle')
  const [geoError,  setGeoError]  = useState('')

  // search state
  const [query,       setQuery]       = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [searching,   setSearching]   = useState(false)
  const [showSuggest, setShowSuggest] = useState(false)
  const debounceRef  = useRef(null)
  const searchBoxRef = useRef(null)

  // ── geolocation ────────────────────────────────────────────────────────────

  const requestGeo = useCallback(() => {
    if (!navigator.geolocation) {
      setGeoStatus('error')
      setGeoError(t('map.geoNotSupported'))
      return
    }
    setGeoStatus('loading')
    setGeoError('')
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const pos = [coords.latitude, coords.longitude]
        setUserPos(pos)
        setSearchCenter(pos)
        setMapTarget({ center: pos, zoom: 13 })
        setGeoStatus('ok')
        setQuery('')
        setSuggestions([])
        setShowSuggest(false)
      },
      (err) => {
        setGeoStatus('error')
        setGeoError(err.code === 1 ? t('map.geoDenied') : t('map.geoError'))
      },
      { enableHighAccuracy: true, timeout: 10000 }
    )
  }, [t])

  useEffect(() => { requestGeo() }, [requestGeo])

  // ── geocode search (debounced 400 ms) ──────────────────────────────────────

  const handleQueryChange = (e) => {
    const val = e.target.value
    setQuery(val)
    setShowSuggest(true)

    clearTimeout(debounceRef.current)
    if (val.trim().length < 2) {
      setSuggestions([])
      setSearching(false)
      return
    }
    setSearching(true)
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await geocodeNL(val)
        setSuggestions(results)
      } catch {
        setSuggestions([])
      } finally {
        setSearching(false)
      }
    }, 400)
  }

  const handleSuggestionPick = (place) => {
    const lat = parseFloat(place.lat)
    const lng = parseFloat(place.lon)
    const label = place.display_name.split(',').slice(0, 2).join(',').trim()
    setQuery(label)
    setSuggestions([])
    setShowSuggest(false)
    setSearchCenter([lat, lng])
    setMapTarget({ center: [lat, lng], zoom: 13 })
  }

  const handleSearchClear = () => {
    setQuery('')
    setSuggestions([])
    setShowSuggest(false)
    if (userPos) {
      setSearchCenter(userPos)
      setMapTarget({ center: userPos, zoom: 13 })
    }
  }

  // close suggestions when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(e.target))
        setShowSuggest(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  // ── load stores: backend DB first, Overpass as fallback ───────────────────

  useEffect(() => {
    const [lat, lng] = searchCenter ?? userPos ?? DEFAULT_CENTER
    let cancelled = false
    setLoading(true)
    setLoadError('')
    setSelected(null)

    async function load() {
      try {
        const { data } = await getNearby(lat, lng, SEARCH_RADIUS)
        if (data.length > 0) return data
      } catch (err) {
        console.warn('[Locations] DB lookup failed, falling back to Overpass:', err.message)
      }
      return fetchBottleReturnStores(lat, lng, SEARCH_RADIUS)
    }

    load()
      .then(stores => { if (!cancelled) setLocations(stores) })
      .catch(err => {
        if (cancelled) return
        console.error(err)
        setLoadError(t('map.overpassError'))
      })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [searchCenter, userPos, t])

  // ── select / deselect store ─────────────────────────────────────────────────

  const handleSelect = useCallback((loc) => {
    setSelected(loc)
    setMapTarget({ center: [loc.lat, loc.lng], zoom: 15 })
  }, [])

  const handleDeselect = useCallback(() => setSelected(null), [])

  const markers = useMemo(() => (
    locations.map(loc => (
      <Marker
        key={loc.id}
        position={[loc.lat, loc.lng]}
        icon={getStoreIcon(loc.chain, selected?.id === loc.id)}
        eventHandlers={{ click: () => handleSelect(loc) }}
      />
    ))
  ), [locations, selected, handleSelect])

  // ── render ─────────────────────────────────────────────────────────────────

  return (
    // Centred app column (same max-w-md mx-auto as HomePage and ProfilePage). Floating
    // overlays are absolute inside this relative container, so they stay within the column.
    <div className="h-screen h-dvh md:h-[calc(100vh-3.5rem)] w-full max-w-md mx-auto relative overflow-hidden">

      {/* map — fills the column */}
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={12}
        style={{ width: '100%', height: '100%' }}
        zoomControl={false}
        attributionControl={false}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="© OpenStreetMap"
          crossOrigin="anonymous"
        />

        {mapTarget && <MapController center={mapTarget.center} zoom={mapTarget.zoom} />}
        <MapClickHandler onMapClick={handleDeselect} />

        <MarkerClusterGroup chunkedLoading showCoverageOnHover={false} maxClusterRadius={60}>
          {markers}
        </MarkerClusterGroup>

        {userPos && (
          <>
            <Marker position={userPos} icon={userIcon} zIndexOffset={1000} />
            <Circle center={userPos} radius={150}
              pathOptions={{ color: '#3b82f6', fillColor: '#3b82f6', fillOpacity: 0.1, weight: 1 }} />
          </>
        )}
      </MapContainer>

      {/* floating top search bar */}
      <div className="absolute top-4 left-4 right-4 z-[1000] md:left-1/2 md:right-auto md:w-full md:max-w-md md:-translate-x-1/2" ref={searchBoxRef}>
        <div className="relative">
          <svg className="absolute left-4 top-3.5 w-4 h-4 text-gray-400 pointer-events-none"
            fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          {searching && (
            <span className="absolute right-4 top-3.5 w-4 h-4">
              <span className="block w-4 h-4 border-2 border-eco-400 border-t-transparent rounded-full animate-spin" />
            </span>
          )}
          {!searching && query.length > 0 && (
            <button onClick={handleSearchClear}
              className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-gray-400 hover:text-gray-600">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          )}
          <input
            className="w-full bg-white border border-gray-200 rounded-2xl pl-10 pr-11 py-3 text-sm shadow-lg shadow-black/10 focus:outline-none focus:ring-2 focus:ring-eco-400"
            placeholder={t('map.searchPlaceholder')}
            value={query}
            onChange={handleQueryChange}
            onFocus={() => suggestions.length > 0 && setShowSuggest(true)}
            autoComplete="off"
          />
        </div>

        {/* suggestions dropdown */}
        {showSuggest && suggestions.length > 0 && (
          <ul className="mt-2 bg-white border border-gray-200 rounded-2xl shadow-lg overflow-hidden max-h-60 overflow-y-auto">
            {suggestions.map((place) => {
              const parts = place.display_name.split(',')
              const main  = parts[0]
              const sub   = parts.slice(1, 3).join(',').trim()
              return (
                <li key={place.place_id}
                  className="flex items-start gap-2 px-4 py-3 hover:bg-eco-50 cursor-pointer border-b border-gray-100 last:border-0"
                  onMouseDown={() => handleSuggestionPick(place)}>
                  <svg className="w-4 h-4 text-eco-500 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                  </svg>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">{main}</p>
                    <p className="text-xs text-gray-400 truncate">{sub}</p>
                  </div>
                </li>
              )
            })}
          </ul>
        )}

        {/* geo status pill (only while locating/erroring — quiet once found) */}
        {(geoStatus === 'loading' || geoStatus === 'error') && (
          <div className="mt-2 inline-flex items-center gap-1.5 bg-white rounded-full px-3 py-1.5 text-xs shadow-md">
            {geoStatus === 'loading' && (
              <>
                <span className="w-3 h-3 border border-gray-400 border-t-transparent rounded-full animate-spin" />
                <span className="text-gray-500">{t('map.locating')}</span>
              </>
            )}
            {geoStatus === 'error' && <span className="text-red-500">{geoError}</span>}
          </div>
        )}
      </div>

      <div className="absolute top-4 right-4 z-[999]" style={{ marginTop: '3.25rem' }}>
        <LanguageSwitcher />
      </div>

      {/* loading / empty-result toast, floating just under the search bar */}
      {(loading || (!loadError && locations.length === 0)) && (
        <div className="absolute top-20 left-1/2 -translate-x-1/2 z-[998] bg-white rounded-full px-4 py-2 text-xs text-gray-500 shadow-md flex items-center gap-2">
          {loading ? (
            <>
              <span className="w-3 h-3 border-2 border-eco-500 border-t-transparent rounded-full animate-spin" />
              {t('common.loading')}
            </>
          ) : (
            t('map.noStores')
          )}
        </div>
      )}
      {loadError && (
        <div className="absolute top-20 left-1/2 -translate-x-1/2 z-[998] bg-white rounded-full px-4 py-2 text-xs text-red-500 shadow-md">
          {loadError}
        </div>
      )}

      {/* OSM attribution — mandatory under the ODbL/tile usage policy since TileLayer's own
          attributionControl is disabled above in favor of this unobtrusive floating badge */}
      <div className={`absolute left-4 z-[900] transition-all ${selected ? 'bottom-56' : 'bottom-24'}`}>
        <LegalAttribution variant="compact" />
      </div>

      {/* floating "my location" GPS button — sits above the store card when one is open */}
      <button
        onClick={requestGeo}
        disabled={geoStatus === 'loading'}
        aria-label={t('map.myLocation')}
        className={`absolute right-4 z-[999] w-12 h-12 rounded-full bg-white shadow-lg shadow-black/10 flex items-center justify-center text-eco-700 disabled:opacity-50 transition-all ${
          selected ? 'bottom-56' : 'bottom-24'
        }`}
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2.2} viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="3"/><path strokeLinecap="round" d="M12 2v3m0 14v3M2 12h3m14 0h3"/>
        </svg>
      </button>

      {/* floating bottom store card */}
      {selected && (
        <div className="absolute left-0 right-0 bottom-0 z-[1000] mx-4 mb-20 max-h-[60vh] overflow-y-auto rounded-3xl shadow-xl bg-white p-5 md:left-1/2 md:right-auto md:mx-0 md:mb-6 md:w-full md:max-w-md md:-translate-x-1/2">
          <div className="flex items-start justify-between gap-3 mb-1">
            <h3 className="font-bold text-gray-900 text-base leading-snug break-words">{selected.name}</h3>
            <button onClick={handleDeselect} className="flex h-11 w-11 flex-shrink-0 items-center justify-center -mr-2 -mt-2 text-gray-300 hover:text-gray-500">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <p className="text-xs text-gray-500 mb-2">{selected.address}</p>
          <div className="flex items-center gap-2 text-xs mb-4">
            <span className="text-eco-600 font-medium">
              🕐 {selected.openUntil ? `${t('map.openUntil')} ${selected.openUntil}` : t('map.hoursUnknown')}
            </span>
            <span className="text-gray-300">·</span>
            <span className="text-gray-400">{formatDistance(selected.distanceMeters)}</span>
          </div>
          <a href={`https://www.google.com/maps/dir/?api=1&destination=${selected.lat},${selected.lng}`}
            target="_blank" rel="noopener noreferrer"
            className="block w-full bg-eco-600 text-white rounded-xl py-3 text-sm font-semibold text-center">
            {t('map.directions')}
          </a>
        </div>
      )}
    </div>
  )
}
