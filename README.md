# EcoLoop — Statiegeld Tracker

A bottle/can deposit-return ("statiegeld") locator and rewards app for the Netherlands. Find nearby
supermarkets that accept deposit returns, scan a product barcode to check whether it is
deposit-eligible and recyclable, confirm your returns, and track your recycling stats, daily
streaks, referrals, and achievement badges.

Built as a set of Java Spring Boot microservices behind an API gateway, with a React + Leaflet
frontend.

## Features

- **Map** — find nearby bottle-return points, search by address, get directions.
- **Scan** — camera-based barcode scanning (ZXing) with an on-device flashlight toggle; falls back
  to manual barcode entry. Every successful scan is kept in your scan history.
- **Deposit check** — a return is credited only when scan-service confirms the product is a
  deposit-eligible beverage container (PET, aluminium, or glass). Each eligible item is worth
  €0.15, stored on the deposit record.
- **Rewards** — points, a gamified "Eco-Sled" level-up tree, daily streaks with check-ins, and
  unlockable achievement badges.
- **Referrals** — every user gets an invite code. The invitee's first deposit credits 50 points to
  both people. Three credited referrals unlock the Eco-Ambassador badge.
- **Return summary** — a 12-month overview per year of items returned, deposit value, and plastic
  recycled.
- **Profile** — four swipeable windows: General Info (points, streaks, growth stage), Referral,
  Medals, and History (scan list and a monthly return chart).
- **Installable web app (PWA)** — installable with a Scan and Map shortcut. Works offline for the
  app shell, the last collection-point list, and map tiles you viewed before. Signed-in data is
  never cached.
- **Responsive layout** — bottom navigation on phones, top bar from medium screens up.
- **i18n** — Dutch, English, and Ukrainian.
- **GDPR-oriented legal pages** — Privacy Policy, Terms of Service, Cookie Policy, and an EU-style
  cookie consent banner, all in `eco-frontend/src/pages/legal/` and
  `eco-frontend/src/components/legal/`.

### Planned (not yet built)

- **Web Push reminders** — re-engagement notifications (see `docs/adr/0004-pwa-and-web-push.md`,
  Phase 2). There is no push subscription and no reminder yet.
- **Offline indicator** — the saved collection-point list is not yet labelled as offline data.
- **Russian (`ru`) translations** — not yet available.
- **End-to-end verification** — the deposit check, the History window, and the PWA are implemented
  but not yet verified end to end in a browser.

## Architecture

```
eco-frontend (React/Vite, :3000/:5173)
        │  /api/*
        ▼
api-gateway (:8080)  — Spring Cloud Gateway, JWT verification, CORS
        │
        ├─ auth-service     (:8081) — registration/login, issues JWTs
        ├─ scan-service     (:8082) — barcode → product/deposit lookup, scan history
        ├─ location-service (:8083) — nearby collection points
        └─ reward-service   (:8084) — points, streaks, referrals, badges, return summary
```

reward-service confirms each deposit with scan-service over an internal endpoint
(`/internal/scan/deposit/{barcode}`). The gateway does not route `/internal/**`. The call is
authenticated with the shared secret `INTERNAL_SERVICE_TOKEN`, which must be set to the same value
in scan-service and reward-service. When it is empty, every deposit is refused.

Each backend service owns its own MySQL database (`eco_auth_db`, `eco_scan_db`,
`eco_location_db`, `eco_reward_db`) — no shared schema between services.

## Tech stack

- **Backend:** Java 17, Spring Boot 3, Spring Cloud Gateway, Spring Data JPA, MySQL 8
- **Frontend:** React 18, Vite, Tailwind CSS, react-leaflet + Leaflet, react-i18next, ZXing (barcode
  scanning), axios, vite-plugin-pwa (offline support), qrcode (referral QR codes)
- **Infra:** Docker / Docker Compose

## External APIs this app uses

None of these require a paid plan, and **none require an API key** — they're all free, open,
community-run services. Each does, however, have a usage policy (rate limits, required attribution,
required `User-Agent` identification) that this app follows — respect those policies if you extend
this project or run it at real scale.

| API | Used for | Where in the code | Auth / key needed | Docs |
|---|---|---|---|---|
| **OpenStreetMap tile servers** | Rendering the map itself (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`) | `eco-frontend/src/pages/MapPage.jsx` (`<TileLayer>`) | None | [Tile usage policy](https://operations.osmfoundation.org/policies/tiles/) |
| **Nominatim** (OpenStreetMap) | Geocoding — turning a typed address/place name into coordinates for search | `eco-frontend/src/pages/MapPage.jsx` (`geocodeNL`) | None, but max **1 request/second** and a descriptive `User-Agent`/referer are required | [Usage policy](https://operations.osmfoundation.org/policies/nominatim/) · [API docs](https://nominatim.org/release-docs/latest/api/Search/) |
| **Overpass API** (OpenStreetMap) | Live fallback supermarket/POI search when our own database has no coverage for an area | `eco-frontend/src/api/overpassApi.js` | None. Public mirrors are unauthenticated but **frequently rate-limited or unreachable** — this app tries several mirrors in sequence (`overpass-api.de`, `overpass.kumi.systems`, and others in the `ENDPOINTS` list) and caches successful results client-side | [Overpass API docs](https://wiki.openstreetmap.org/wiki/Overpass_API) |
| **Open Food Facts** | Product lookup by barcode (name, brand, packaging material) | `scan-service/src/main/java/com/ecoapp/scan/client/OpenFoodFactsClient.java` | None, but requests must send a descriptive `User-Agent` (see `AppConfig.java`) | [API docs](https://openfoodfacts.github.io/openfoodfacts-server/api/) · [Privacy policy](https://world.openfoodfacts.org/privacy) |

`location-service`'s own database (`collection_points`) is the **primary** source for the Map
page — it's seeded with real, OpenStreetMap-derived supermarket data (see
`location-service/src/main/resources/data.sql`). Overpass is only queried live as a fallback for
areas outside that seed data.

There is also an unused `@googlemaps/js-api-loader` reference that has been removed — an earlier
prototype considered the Google Maps JavaScript API (which **does** require a billed API key from
[Google Cloud Console](https://console.cloud.google.com/google/maps-apis)), but the app was built
entirely on Leaflet/OpenStreetMap instead, so no Google Maps key is needed anywhere in this project.

## Getting started

### Prerequisites

- Docker + Docker Compose (recommended path), **or** Java 17 + Maven + a local MySQL 8 instance
  (manual path)
- Node.js 18+ and npm, for the frontend

### 1. Configure secrets

Copy the example env file and fill in real values (or keep the placeholders for local-only use):

```bash
cp .env.example .env
```

```
DB_PASSWORD=changeme                                          # MySQL root password, shared by all 4 databases
JWT_SECRET=changeme-generate-a-real-secret-before-any-real-deployment
INTERNAL_SERVICE_TOKEN=                                       # shared by scan-service and reward-service; empty refuses all deposits
```

Generate a real `JWT_SECRET` with:

```bash
openssl rand -base64 48
```

Generate a real `INTERNAL_SERVICE_TOKEN` with `openssl rand -base64 32`. Put the same value in
`.env` once; both services receive it from there. Until it is set, reward-service refuses every
deposit (fail closed).

`.env` is gitignored — never commit real secrets. `docker-compose.yml` reads this file
automatically.

### Environment variables

Names and purpose only. Values belong in `.env` or your run configuration, never in tracked files.

| Variable | Used by | Purpose |
|---|---|---|
| `DB_PASSWORD` | docker-compose (MySQL containers) | MySQL root password for the four databases |
| `JWT_SECRET` | auth-service, api-gateway | Key that signs and verifies JWTs |
| `JWT_EXPIRATION_MS` | auth-service | Lifetime of an issued JWT, in milliseconds |
| `INTERNAL_SERVICE_TOKEN` | scan-service, reward-service | Shared secret for the internal deposit check. Must be identical in both services. Empty refuses all deposits |
| `SCAN_SERVICE_URL` | reward-service | Base URL of scan-service (default `http://localhost:8082`) |
| `AUTH_SERVICE_URL`, `LOCATION_SERVICE_URL`, `REWARD_SERVICE_URL` | api-gateway | Base URLs of the services the gateway routes to |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | each service | JDBC URL, user, and password of the service's own database |

### 2. Run the backend (Docker Compose)

```bash
docker compose up -d --build
```

This starts 4 MySQL containers plus the 5 Spring Boot services. The gateway is reachable at
`http://localhost:8080`.

### 3. Run the frontend

```bash
cd eco-frontend
npm install
npm run dev
```

Open `http://localhost:3000`. The Vite dev server proxies `/api` to the gateway on `:8080`.

### Running backend services individually (without Docker)

Each service reads its DB password and (for `auth-service`/`api-gateway`) JWT secret from
`SPRING_DATASOURCE_PASSWORD` / `JWT_SECRET` environment variables, falling back to the harmless
placeholders `changeme` / `changeme-local-dev-jwt-secret-do-not-use-in-production` if unset (see
each service's `src/main/resources/application.yml`). If your local MySQL doesn't use `changeme`
as its root password, either:

- export `SPRING_DATASOURCE_PASSWORD=<your real local password>` in your shell/IDE run
  configuration, **or**
- create a `src/main/resources/application-local.yml` per service (already gitignored) with your
  real local values, and run with `-Dspring.profiles.active=local`.

When you run scan-service and reward-service outside Docker, set `INTERNAL_SERVICE_TOKEN` to the
same value in both. Without it, reward-service refuses every deposit. If scan-service is not on
`http://localhost:8082`, also set `SCAN_SERVICE_URL` for reward-service.

## Project structure

```
api-gateway/       Spring Cloud Gateway — JWT verification, routing, CORS
auth-service/      Registration/login, issues JWTs
scan-service/      Barcode → product/deposit lookup (Open Food Facts), deposit check, scan history
location-service/  Nearby collection points (own DB + live Overpass fallback)
reward-service/    Points, streaks and check-ins, referrals, badges, stats, return summary
eco-frontend/      React/Vite PWA
docker-compose.yml Local multi-service orchestration
```

## Legal

See the in-app [Privacy Policy](eco-frontend/src/pages/legal/PrivacyPolicy.jsx),
[Terms of Service](eco-frontend/src/pages/legal/TermsOfService.jsx), and
[Cookie Policy](eco-frontend/src/pages/legal/CookiePolicy.jsx). These are drafted to reflect this
app's actual data handling but are **templates, not legal advice** — every `[BRACKETED]` placeholder
(company name, KVK number, DPO contact, hosting provider) needs to be filled in and reviewed by a
qualified lawyer before handling real users' data.

