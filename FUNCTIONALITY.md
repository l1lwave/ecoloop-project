# EcoLoop — Functional Reference

A complete inventory of what this app actually does: every screen, every button, every API
endpoint, and every business rule, as implemented in the code today. (See `README.md` for
setup/architecture; this document is the functional deep-dive.)

---

## 1. What the app is

EcoLoop is a Dutch **statiegeld** (bottle/can deposit-return) companion app. A logged-in user can:
find nearby supermarkets that accept deposit returns, scan a product barcode to check whether it's
recyclable and what it's worth, confirm they returned it to earn points (only after the barcode is
verified as deposit-eligible), keep a daily streak, invite friends, and track their recycling
stats, return history and badges over time.

---

## 2. User-facing features (frontend, `eco-frontend/`)

### 2.1 Authentication — `LoginPage.jsx`, `RegisterPage.jsx`, `AuthContext.jsx`
- **Register**: name, email, password (min 6 chars) → creates an account, immediately logs the user
  in (backend returns a JWT on registration, no separate login step needed).
- **Login**: email + password → JWT.
- The JWT and basic user info (`userId`, `name`, `email`) are stored in `localStorage`
  (`eco_token`, `eco_user`), attached as `Authorization: Bearer <token>` to every subsequent API
  call (`axiosConfig.js`). A 401 response anywhere in the app clears storage and redirects to
  `/login`.
- **Route protection**: `ProtectedRoute.jsx` gates `/`, `/map`, `/scan`, `/profile` behind having a
  logged-in `user`; `/login`, `/register`, and the three legal pages are public.
- Small legal-links line under the login form (Privacy Policy / Terms of Service).
- **Sign-in side effects** (mounted globally, run while a user is signed in):
  - `DailyCheckin.jsx` sends one check-in per signed-in user per page load
    (`POST /api/rewards/checkin`). An in-memory guard prevents repeats until the next reload. Errors
    are ignored so they never block the app.
  - `ReferralClaim.jsx` reads a `?ref=` code from the URL and claims it once, after sign-in
    (`POST /api/rewards/referrals/claim`). The `ref` parameter is removed from the URL afterwards,
    and invalid-looking values are removed without a claim attempt.

### 2.2 Home — `HomePage.jsx`
- Greets the user by name, shows current total points.
- One large circular **"Scan object"** button → `/scan`.
- **Weekly impact card**: plastic recycled this week (kg), as a progress bar out of a 15 kg
  reference target.
- **"Nearest point" card**: shortcut to `/map`.
- **Recent badges strip**: up to 3 unlocked badges, most-recently-relevant first (as returned by
  the backend), each shown with its translated name (see §4.3).
- Language switcher + logout button.

### 2.3 Map — `MapPage.jsx`
- Full-screen Leaflet map (OpenStreetMap tiles) with floating UI on top: search bar, GPS button,
  bottom store-detail sheet — no traditional page chrome.
- **Geolocation**: on load, requests the browser's GPS position; shows a small status pill while
  locating, or an error if denied/unsupported. A GPS marker + accuracy circle render for the user's
  real position.
- **Address search**: debounced (400 ms) autocomplete against OpenStreetMap Nominatim, restricted
  to the Netherlands; picking a suggestion re-centers the map and re-runs the store search from
  that point.
- **Store loading, DB-first / Overpass-fallback**: for the current center point, calls
  `GET /api/locations` (backend DB) first; if that returns zero results for the area, falls back to
  a **live Overpass API query** (multiple public mirrors tried in sequence, client-side cached for
  15 minutes) so areas outside the seeded dataset still show real OpenStreetMap supermarket data.
- **Clustering**: markers are grouped via `react-leaflet-cluster` once they're close together on
  screen; each pin is colored per supermarket chain (Albert Heijn, Jumbo, Lidl, Aldi, Plus, Spar,
  Coop, Dirk, Vomar, DekaMarkt — see `CHAIN_HEX` in `MapPage.jsx`).
- **Material filter chips** were removed (all seeded return points accept the same materials, so
  filtering by material was redundant) — the map shows all stores in range.
- Tapping a marker opens a bottom sheet: store name, address, opening hours (or an honest "hours
  not available" if the source data doesn't have them), distance from the user, and a **"Get
  directions"** button that opens Google Maps directions in a new tab. Tapping the map background
  or the sheet's close button dismisses it.
- A small **"© OpenStreetMap contributors"** attribution badge floats over the map (legally
  required — see `LegalAttribution.jsx`).

### 2.4 Scan — `ScanPage.jsx`
- **Camera scanning**: starts the device camera (rear-facing preferred) and continuously decodes
  barcodes via ZXing. On a successful read, stops the camera and looks up the product.
- **Flashlight/torch toggle**: appears only if the active camera track reports torch support
  (mainly Android Chrome); toggles the device's actual hardware torch via
  `MediaStreamTrack.applyConstraints({advanced:[{torch}]})`.
- **Manual entry fallback**: a numeric input (8–14 digits) plus a grid of demo barcodes, for
  devices without a usable camera or for quick testing.
- **Product result**: name, brand, recyclable ✅/❌ badge, material type, deposit value (€) if
  applicable, and localized care instructions (NL/EN/UA).
- **Scan history**: every successful scan for a signed-in user is stored (see §4.8).
- **Two actions once a result is shown**:
  - **"Find point"** — opens the Map page in a **new browser tab** (deliberately not an in-app
    navigation, so the scanned result isn't lost while the user goes to find a return point).
  - **"Returned"** (shown only when the product has a deposit value above zero, i.e. it is
    deposit-eligible) — confirms the deposit: calls the rewards API, which first verifies the barcode
    with scan-service (see §4.7), then credits points, updates recycling stats and streaks, and may
    unlock new badges. The success screen shows points earned and any newly unlocked badge names.
- **Recyclable but not deposit-eligible** products show a note explaining why there is no "Returned"
  button, instead of the button.
- "Scan again" resets the flow at any point.

### 2.5 Profile — `ProfilePage.jsx`
The page holds four windows, shown as tabs and navigable by swiping (`ProfileTabs.jsx`,
`SwipeableWindows.jsx`). The swipe order is General Info, Referral, Medals, History.

**Window 1 — General Info** (`GeneralInfoPanel.jsx`)
- **Eco-Sled tree**: a big emoji that changes across 10 growth stages (🌱 → 🌏) based on the user's
  level (see §4.2), with a progress bar toward the next 500-point milestone (`GrowthStageRow.jsx`).
- **Streaks**: current streak and longest streak (see §4.5).
- **Stat cards** (`StatCard.jsx`): plastic recycled (kg), water saved (L), locations visited, and
  "eco-checks" (total successful deposits).

**Window 2 — Referral** (`InviteFriendCard.jsx`)
- The user's own invite link, a QR code for the same link, a copy button, and an optional share
  button.
- Counts of credited and pending referrals, and whether the Eco-Ambassador badge is unlocked (see
  §4.6).

**Window 3 — Medals** (`MedalsPanel.jsx`)
- All badges the backend knows about. Unlocked badges glow with their translated name. Locked badges
  are greyed out with a 🔒.

**Window 4 — History** (`HistoryPanel.jsx`)
- **Scan list** (`ScanHistoryList.jsx`): the user's scans, newest first, 20 per page, with a "load
  more" button (see §4.8). **Note: implemented, not yet verified end to end.**
- **Monthly return chart** (`ReturnChart.jsx`, `ReturnHistory.jsx`): items returned, deposit value,
  and plastic per month, with previous/next-year buttons to move between years (see §4.8). **Note:
  implemented, not yet verified end to end.**

The page ends with a full legal-attribution footer (OpenStreetMap/Open Food Facts credits,
trademark disclaimer, links to Privacy/Terms/Cookie pages).

### 2.6 Legal pages — `pages/legal/`, `components/legal/`
- **Privacy Policy**, **Terms of Service**, **Cookie Policy** — GDPR/ePrivacy-oriented drafts
  reflecting this app's actual data handling (ephemeral GPS, on-device camera processing, JWT in
  localStorage not cookies, no analytics). Marked as templates needing a real legal review before
  handling real users' data.
- **`CookieConsent.jsx`** — a bottom banner (shown once, remembered in `localStorage`) explaining
  what's stored and why; includes a real, currently-unused opt-in toggle scaffold for a future
  "Analytics" category, off by default.
- **`LegalAttribution.jsx`** — compact (map overlay) and full (footer) variants of the mandatory
  OpenStreetMap/Open Food Facts attributions and the supermarket-trademark disclaimer.

### 2.7 Shared chrome
- **`BottomNav.jsx`** — navigation (Home / Map / Scan / Profile), hidden on login/register/legal
  pages. It is a fixed bottom bar on phones and a top bar from the `md` breakpoint up.
- **`LanguageSwitcher.jsx`** — toggles the UI between Dutch, English, and Ukrainian
  (`react-i18next`), persisted in `localStorage` (`eco_lang`).
- **Responsive layout** — page content sits in a centred `max-w-md` column. The Map and Scan
  pages use the same column. Interactive controls are at least 44 px tall or wide for touch.

### 2.8 Installable app, offline support, and install banner — `vite.config.js`, `InstallBanner.jsx`
- **Installable**: the web app manifest sets a standalone display mode, the EcoLoop name, and Scan
  and Map shortcuts. Icons include a maskable variant.
- **Offline support** (service worker, `vite-plugin-pwa`):
  - **App shell**: the built HTML, JavaScript, CSS, and image assets are precached. Navigating to any
    UI route offline returns the app shell. `/api/*` requests are never answered with the shell.
  - **Collection points**: the last successful `GET /api/locations` response is kept for 24 hours.
    There is one stored entry, and it is shared by all users of the device, because collection
    points are the same for everyone. The entry is used when the network is unavailable.
  - **Map tiles**: OpenStreetMap tiles you viewed before are kept for up to 7 days, with a limit of
    500 tiles. Only successful responses are stored.
  - **Never cached**: rewards, scan, and auth routes, and any request that carries an
    `Authorization` header (except collection points). A shared device can therefore never show one
    user's data to the next user.
- **Install banner** (`InstallBanner.jsx`): appears only when the browser offers installation
  (`beforeinstallprompt`). Browsers that never offer it (for example Safari on iOS) show no banner.
  The banner state is kept in memory only and is not written to storage.
- **Note**: the PWA is implemented, not yet verified end to end in a browser.

---

## 3. Backend services & API reference

All traffic goes through **`api-gateway` (:8080)**, which:
- Verifies the JWT on every request except `/api/auth/login` and `/api/auth/register`, rejecting
  unauthenticated requests with 401.
- Adds a trusted `X-User-Id` header (from the verified JWT's subject) to every proxied request,
  stripping any client-supplied value of the same name first, so downstream services can trust it.
- Rate-limits `/api/auth/login` and `/api/auth/register` (10 requests/minute/IP, in-memory).
- Adds security response headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options,
  Permissions-Policy, Referrer-Policy) to every response.
- Routes: `/api/auth/**` → auth-service, `/api/scan/**` → scan-service, `/api/locations/**` →
  location-service, `/api/rewards/**` → reward-service.

### 3.1 `auth-service` (:8081) — accounts & JWTs
| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/auth/register` | public | `{name, email, password}` → creates a user (BCrypt-hashed password), returns `{token, userId, name, email}` |
| `POST /api/auth/login` | public | `{email, password}` → same response shape as register |
| `GET /api/auth/me` | JWT | Returns the caller's own profile (`userId, name, email, createdAt`) — identity comes from the service's own independently-verified JWT, not a trusted header, as defense in depth |

### 3.2 `scan-service` (:8082) — barcode → product lookup
| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/scan/identify` | required¹ | `{barcode, lang}` → product info (see lookup logic in §4.1). Each successful scan is stored as a scan event for the caller |
| `GET /api/scan/materials` | required¹ | List of all known material types and their recyclability |
| `GET /api/scan/history?page&size` | required¹ | The caller's own scans, newest first. `page` defaults to 0, `size` to 20; `size` is capped at 50 and must be at least 1 (see §4.8) |
| `GET /internal/scan/deposit/{barcode}` | shared token² | Internal deposit check used by reward-service (see §4.7). Not routed by the gateway |

¹ Enforced via a `RequireUserIdFilter` that rejects requests missing the gateway's trusted header —
a backstop in case this service is ever reachable outside the gateway. The user ID is used only to
store and read the caller's own scan events.

² Enforced via an `InternalTokenFilter`. The caller sends the token in the `X-Internal-Token` header,
and it must equal the `INTERNAL_SERVICE_TOKEN` environment variable (compared in constant time). The
same value must be set in reward-service. When the variable is empty, every internal call is refused
with 401. The check reads only scan-service's own `products` table; it never calls Open Food Facts
(see §4.7).

### 3.3 `location-service` (:8083) — collection points
| Endpoint | Auth | Description |
|---|---|---|
| `GET /api/locations?lat&lng&radius&material` | JWT (via gateway) | Nearby collection points within `radius` meters, sorted by distance; `lat`/`lng` validated to real coordinate ranges, `radius` capped at 50 km |
| `GET /api/locations/{id}?lat&lng` | JWT (via gateway) | One collection point by ID, with computed distance from the given point |

Public data — no per-user scoping needed (see §5 for the seed dataset).

### 3.4 `reward-service` (:8084) — points, stats, badges
| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/rewards/deposit` | required¹ | `{locationId?, barcode?, materialType}` → first checks the barcode with scan-service; a product that is not deposit-eligible is refused (§4.7). Otherwise credits points/stats for **the caller's own account** (userId comes only from the trusted header — never from the request body, closing an earlier IDOR where a client could credit another account) |
| `POST /api/rewards/checkin` | required¹ | Records today's activity for the caller (idempotent: a second call on the same day changes nothing). Returns the current and longest streak and any newly unlocked badges (§4.5) |
| `GET /api/rewards/profile` | required¹ | The caller's own stats (including `currentStreak` and `longestStreak`) + full badge list (no path parameter — this endpoint can only ever return your own data) |
| `GET /api/rewards/leaderboard?limit=10` | required¹ | Top users by total points (no PII exposed — just `userId`, stats, badges) |
| `GET /api/rewards/referrals/me` | required¹ | The caller's invite code, `creditedCount`, `pendingCount`, and `ambassadorUnlocked`. Invitees are never identified (§4.6) |
| `POST /api/rewards/referrals/claim` | required¹ | `{code}` → creates a pending referral for the caller. Every rule violation returns the same generic error (§4.6) |
| `GET /api/rewards/returns/summary?year=YYYY` | required¹ | Twelve monthly entries for the caller: items returned, deposit EUR, and plastic kg, plus year totals (§4.8) |

¹ Same `RequireUserIdFilter` backstop as scan-service.

---

## 4. Business logic

### 4.1 Product/material identification (`scan-service`)
1. Check the local `products` table (seeded demo products + anything looked up before).
2. If not found, call **Open Food Facts** by barcode, requesting only the fields actually used
   (name in NL/EN/UK, brand, categories, packaging text/tags, quantity) for speed.
3. Infer a material code from (in priority order): `packaging_materials_tags` → `categories_tags`
   (packaging phrases only, such as "beverage cans" or "glass bottles") → free-text packaging
   description. If nothing matches, the material is `unknown`. Unknown packaging is **never**
   assumed to be PET.
4. Mark the product as a **beverage** if one of its category tag segments is a beverage word (for
   example beverages, water, soda, juice, beer, drink). Whole segments only, so "pet food" is not a
   beverage.
5. **Deposit eligibility** (€0.15, matching NL statiegeld rules): a product is eligible only if it is
   a beverage **and** its material is **PET plastic**, **aluminium**, or **glass**. Everything else
   has a deposit value of 0 but still gets a recyclability verdict and care instructions. Seeded
   demo drinks carry the beverage flag.
6. The looked-up result is cached in the local `products` table (including the beverage flag) so the
   next scan of the same barcode skips the external call entirely. When a cached row is read, its
   deposit value is recalculated from the current rules. A cached row made before the beverage flag
   existed is re-checked against Open Food Facts. If Open Food Facts no longer knows the product, the
   row is returned with deposit 0 and is not saved again.
7. Every successful lookup is also written to `scan_events` for the signed-in user (§4.8). A failure
   to write the event is logged without the barcode or user ID, and the scan result is still returned.

Materials known to the system (`materials` table): `01 PET`, `Alu`, `02 HDPE`, `04 LDPE`, `05 PP`,
`06 PS`, `GL` (glass), `Fe` (steel/tin) — each with per-material NL/EN/UA disposal instructions and
a recyclable flag.

### 4.2 Points, levels, and stats (`reward-service`)
- **Points per deposit**: 50 for aluminum, 50 for PET (flat rate, `POINTS_PER_ALU_CAN` /
  `POINTS_PER_PET_BOTTLE` in `RewardService.java`).
- Each deposit also adds: +1 eco-check, +30 g to plastic-recycled totals (both lifetime and
  weekly), +3.7 L to water-saved total (illustrative constants, not measured per-product).
- Each deposit records its deposit value (€0.15 for an eligible item) on the deposit record. The value
  comes from scan-service, never from the client (§4.7). Returns recorded before the deposit check
  existed have no stored value and count as items returned, but add no money.
- The first deposit of an invited user also pays a 50-point referral bonus to both users (§4.6).
- Each deposit counts as activity for today's streak (§4.5).
- **Eco-Sled level** (`UserStats.getEcoSledLevel()`), purely a function of lifetime points:

  | Points | 0 | 50 | 200 | 400 | 700 | 1000 | 1500 | 2000 | 3000 | 5000 |
  |---|---|---|---|---|---|---|---|---|---|---|
  | Level | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 10 |

  (Level 9's threshold is unused — jumps straight from 8 to 10 at 5000 pts; the frontend has 10
  tree emoji stages, `ECO_SLED_STAGES` in `ProfilePage.jsx`.)

### 4.3 Achievements (11 badges, `reward-service`'s `badges` table)
Checked after every deposit and every check-in; a badge unlocks the first time its condition is met
and stays unlocked. Badges are never revoked. Names/descriptions are translated client-side
(`badges.<id>.name` in the locale files) — the backend only ever deals in stable IDs.

| ID | Unlocks when |
|---|---|
| `first-checkin` | 1st deposit |
| `recycling-pro` | 200 total points |
| `ocean-saver` | 10 deposits |
| `plastic-pack-1` | 25 deposits |
| `plastic-pack-2` | 50 deposits |
| `glass-master` | 3 different locations visited |
| `battery-champion` | 1000 total points |
| `eco-family` | 30-day streak (30 days in a row, see §4.5). Users who already had it keep it |
| `green-hero` | 3000 total points |
| `sorting-master` | 20 deposits |
| `eco-ambassador` | 3 credited referrals (see §4.6) |

**Known description/logic mismatches** (the seeded Dutch copy promises something more specific than
`checkAndUnlockBadges()` actually checks — worth reconciling if the wording matters to you):
- `sorting-master` — description says "scan 20 different products"; the actual check is **20
  deposits total**, not 20 distinct barcodes.
- `plastic-pack-1` / `plastic-pack-2` — descriptions say "25/50 PET bottles"; the actual check is
  **25/50 deposits of any material** (`depositRepo.countByUserId`, not filtered by material type).

### 4.4 Map data sourcing (`location-service` + `overpassApi.js`)
- Primary source: `collection_points` table, seeded with ~944 real, OpenStreetMap-derived
  supermarkets across ~50 Dutch cities/towns (real names/addresses/coordinates; opening hours left
  blank rather than fabricated where unknown).
- Fallback: live Overpass API query for any search area with zero local results, so the app still
  works outside the seeded cities whenever the public Overpass mirrors are reachable (they're
  sometimes not — see the `[[overpass-api-unreliable]]` memory note).

### 4.5 Daily streaks and check-ins (`reward-service`, `StreakService`)
- A day is **active** when the user checks in or makes a deposit. Days are the local date in the
  **Europe/Amsterdam** time zone.
- `POST /api/rewards/checkin` is idempotent: a second call on the same day changes nothing. The
  frontend sends one check-in per signed-in user per page load (§2.1).
- An active day right after the previous active day extends the streak by one. After a gap, the
  streak starts again at 1 on the next activity. The longest streak is kept.
- The profile shows `currentStreak` and `longestStreak`. The shown current streak is **0** when the
  last active day is older than yesterday. The stored value is corrected on the next activity.
- The `eco-family` badge unlocks at a 30-day streak (§4.3).

### 4.6 Referrals (`reward-service`, `ReferralService`)
1. Each user has one invite code of 8 characters. It is created on first request and then stays the
   same. The alphabet leaves out 0, O, 1, I, and L.
2. An invitee claims a code with `POST /api/rewards/referrals/claim` (`{code}`). This creates a
   **pending** referral. No points are paid at this point.
3. The invitee's **first deposit** credits the referral: **50 points to each** of the two users. The
   payout happens once.
4. A claim is rejected when the code is the user's own, the user already has a referral, the code is
   unknown, or the invitee already has deposits. All these cases return the same generic error, so
   the response never shows which rule failed or whether a code exists.
5. `GET /api/rewards/referrals/me` shows the referrer only the code, `creditedCount`, `pendingCount`,
   and `ambassadorUnlocked`. Invitees are never identified.
6. Three credited referrals unlock the `eco-ambassador` badge (§4.3).
7. Frontend: the Referral window on the profile page (§2.5) and the `?ref=` claim on sign-in (§2.1).

### 4.7 Deposit verification (`reward-service` calls `scan-service`)
1. A deposit (`POST /api/rewards/deposit`) first asks scan-service whether the barcode is
   deposit-eligible, using the internal endpoint and the shared `INTERNAL_SERVICE_TOKEN`. Nothing is
   credited before this check passes.
2. A product that is not eligible is refused with **400 `NOT_RETURNABLE`**.
3. When the check cannot be completed, the deposit is refused with **503 `DEPOSIT_UNAVAILABLE`**. This
   covers a missing or wrong token, an unreachable scan-service, a timeout, and an unexpected reply.
   The check fails closed.
4. The deposit value (€0.15 for an eligible item) comes from scan-service and is stored on the deposit
   record. The client never sends a value.
5. scan-service checks only its own `products` table. A barcode that has never been looked up by a
   scan is therefore treated as not eligible until someone scans it.
6. Points, stats, streaks, referral bonuses, and badges are updated only after the check passes. The
   scan-service call runs outside any database transaction, so no row lock is held during the network
   call.
7. **Note: implemented, not yet verified end to end in a browser.**

### 4.8 Scan history and return summary (`scan-service`, `reward-service`)
- **Scan history**: every successful scan for a signed-in user is stored as a scan event (barcode,
  product name, material, deposit value, and time). `GET /api/scan/history?page&size` returns one
  page, newest first. `page` defaults to 0 and `size` to 20. `size` is capped at 50.
- **Return summary**: `GET /api/rewards/returns/summary?year=YYYY` returns exactly 12 calendar months
  for the given year, using Europe/Amsterdam month boundaries. Each month shows the items returned
  (deposit records), the deposit total in EUR, and the plastic in kg (30 g per returned item).
  The response also shows year totals for items, EUR, and kg. The year must be between 2000 and 2100.
- **Money counts only returns with a stored deposit value.** Older returns recorded before the deposit
  check existed count as items and plastic, but add no EUR.
- **Frontend**: the History window (§2.5) shows the scan list and the monthly chart, with
  previous/next-year buttons. **Note: implemented, not yet verified end to end in a browser.**

---

## 5. Data model (one MySQL schema per service — no cross-service joins)

- **`eco_auth_db`** — `users` (id [UUID], name, email [unique], password [BCrypt hash], created_at)
- **`eco_scan_db`**
  - `products` (barcode [PK], product name, brand, material code, deposit value, volume in ml, and
    a **beverage** flag that records whether the product is a beverage)
  - `materials` (code [PK], label, recyclable, instructions × 3 languages)
  - `scan_events` — one row per successful scan for a signed-in user: user, barcode, product name,
    material code, deposit value, and scan time. Used for scan history (§4.8)
- **`eco_location_db`** — `collection_points` (id, name, chain, address [unique], lat, lng,
  open_until, accepted_materials, machine_count)
- **`eco_reward_db`**
  - `user_stats` (user ID [PK], points, plastic and water totals, locations visited, eco-checks,
    weekly plastic, and the streak columns: current streak, longest streak, and the last active
    date)
  - `badges` (ID [PK], name, description, and the unlock thresholds: points, deposits, locations,
    **streak days**, and **referrals**)
  - `user_badges` (user ID + badge ID — which badges each user has earned)
  - `deposit_records` (history of every deposit: user, location, barcode, material type, points
    earned, time, and the **deposit value** in EUR. The deposit value is empty for deposits made
    before the deposit check existed)
  - `daily_activity` (user ID + activity date, one row per user per active day; this is the basis
    of the streak)
  - `referral_codes` (one invite code per user, unique)
  - `referrals` (invited user, referrer, status pending or credited, creation time, and credit
    time). An invited user can have only one referral.

---

## 6. Cross-cutting

- **i18n**: every user-facing string in the frontend goes through `react-i18next`; Dutch, English,
  Ukrainian locale files under `eco-frontend/src/i18n/locales/`.
- **Security**: JWT auth (HS-family signed, `io.jsonwebtoken`), BCrypt password hashing, per-service
  `RequireUserIdFilter` backstops, gateway-level rate limiting + security headers, DTO-based request
  binding everywhere (no mass-assignment surface), parameterized JPA queries throughout (no raw SQL
  concatenation anywhere in the codebase). Service-to-service deposit checks use the
  `INTERNAL_SERVICE_TOKEN` shared secret, compared in constant time, and fail closed when it is empty.
- **Time zone**: streaks and monthly return totals use Europe/Amsterdam calendar days and months.
- **Error responses**: deposit refusals return a short error code and message (for example
  `NOT_RETURNABLE`), never a stack trace or internal detail.
- **No payment processing, no file uploads, no cookies** — auth is JWT-in-localStorage, not
  session cookies; there is nothing in this app that accepts an uploaded file.

---

## 7. Planned (not yet built)

These items are **not** working features. Do not rely on them yet.

- **Web Push re-engagement reminders** (`docs/adr/0004-pwa-and-web-push.md`, Phase 2). There is no
  push subscription and no reminder.
- **Offline indicator** on the collection-point list. The saved list is used offline, but it is not
  yet labelled as offline data.
- **Russian (`ru`) translations.** Not available yet; the app is in Dutch, English, and Ukrainian.
- **End-to-end verification** in a browser of the deposit check (§4.7), the History window (§2.5),
  and the PWA (§2.8). These are implemented but not yet verified end to end.
