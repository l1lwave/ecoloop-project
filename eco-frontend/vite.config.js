import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// Offline caching rules from docs/adr/0004-pwa-and-web-push.md, decision 2.
//
// vite-plugin-pwa serializes the runtimeCaching functions into sw.js as text.
// Each function below must therefore be self-contained: it must not refer to
// module-level variables or imports.
const runtimeCaching = [
  {
    // Collection points: the last successful GET /api/locations response.
    // Network first, with the cached copy as the offline fallback.
    // Cached even when an Authorization header is sent: collection points are public and identical
    // for every user, so the stored copy cannot reveal one user's data to another. This entry comes
    // before the NetworkOnly exclusion below so that signed-in requests are cached too.
    urlPattern: ({ url }) => url.pathname === '/api/locations',
    handler: 'NetworkFirst',
    method: 'GET',
    options: {
      cacheName: 'eco-collection-points',
      // The query string (lat, lng, radius, material) is removed from the cache key, so one entry
      // holds the last successful list instead of one entry per position.
      plugins: [
        {
          cacheKeyWillBeUsed: async ({ request }) => {
            const url = new URL(request.url)
            return `${url.origin}${url.pathname}`
          },
        },
      ],
      // Limits: one entry, and it expires after 24 hours (24 * 60 * 60 seconds).
      expiration: { maxEntries: 1, maxAgeSeconds: 24 * 60 * 60 },
      cacheableResponse: { statuses: [200] },
    },
  },
  {
    // NEVER cached. Responses for /api/rewards/**, /api/scan/**, /api/auth/** depend on the
    // signed-in user. A device can be shared between accounts, so a stored copy could show one
    // user's data to the next user. Any other request with an Authorization header is treated the
    // same way. This entry uses NetworkOnly, so nothing stores these requests.
    // Collection points (/api/locations) are handled by the entry above and are not affected here.
    urlPattern: ({ url, request }) =>
      /^\/api\/(rewards|scan|auth)(\/|$)/.test(url.pathname) || request.headers.has('authorization'),
    handler: 'NetworkOnly',
  },
  {
    // Map tiles from OpenStreetMap (hosts a, b, c, as used by Leaflet's {s} placeholder).
    // Cache first. The limits respect the OSM tile usage policy:
    //   maxEntries 500, maxAgeSeconds 7 days (7 * 24 * 60 * 60).
    // Only status 200 is stored. The TileLayer in MapPage.jsx sets crossOrigin="anonymous", so tile
    // requests are CORS requests and return normal 200 responses that can be stored.
    urlPattern: /^https:\/\/[abc]\.tile\.openstreetmap\.org\//,
    handler: 'CacheFirst',
    options: {
      cacheName: 'eco-map-tiles',
      expiration: { maxEntries: 500, maxAgeSeconds: 7 * 24 * 60 * 60 },
      cacheableResponse: { statuses: [200] },
    },
  },
]

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      // The new service worker activates and reloads open pages. Documented in ADR 0004 consequences.
      registerType: 'autoUpdate',
      manifest: {
        name: 'EcoLoop',
        short_name: 'EcoLoop',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        // Same green as eco-600 in tailwind.config.js and the theme-color meta in index.html.
        theme_color: '#16a34a',
        // Same as the body background (eco-50).
        background_color: '#f0fdf4',
        lang: 'nl',
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          { src: '/icons/icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
        shortcuts: [
          { name: 'Scan', short_name: 'Scan', url: '/scan' },
          { name: 'Map', short_name: 'Map', url: '/map' },
        ],
      },
      workbox: {
        // Precache the app shell and the built assets.
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        // Offline navigation to any UI route returns the app shell. API calls are never rewritten.
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api\//],
        skipWaiting: true,
        clientsClaim: true,
        cleanupOutdatedCaches: true,
        runtimeCaching,
      },
    }),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/overpass': {
        target: 'https://overpass.kumi.systems',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/overpass/, '/api/interpreter'),
        secure: true,
      },
      '/overpass2': {
        target: 'https://overpass-api.de',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/overpass2/, '/api/interpreter'),
        secure: true,
      },
    },
  },
})
