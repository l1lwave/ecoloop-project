# ADR 0004: PWA offline support and Web Push re-engagement

Status: Proposed
Date: 2026-10-29

## Context

EcoLoop should install on phones and desktops, work offline for the parts that do not need the
server, and remind inactive users to return their deposit bottles and cans.

Three constraints shape the design:
- Service workers and push need HTTPS or `localhost`.
- Web Push needs a VAPID key pair. The private key is a secret: it goes in environment variables
  and never in tracked files (CLAUDE.md, Secrets and configuration).
- A push subscription (endpoint and keys) identifies a device and is personal data. It is stored
  only after explicit opt-in, and it is deleted on unsubscribe.

## Decision

1. **Frontend PWA (eco-frontend).** Use `vite-plugin-pwa` with Workbox, pinned to an exact version
   at least two weeks old. Reason: it is the standard Vite integration for Workbox, and it is
   cheaper than hand-written cache logic.
2. **What is cached offline.**
   - Static assets (app shell) and the UI routes: precached.
   - Collection points: the last successful `GET /api/locations` response, network-first with a
     cached fallback.
   - Map tiles: cache-first, with a fixed maximum entry count and age, to respect the OSM tile
     usage policy.
   - **Never cached:** any response that depends on the signed-in user (`/api/rewards/**`,
     `/api/scan/history`, auth routes). On a shared device this would leak data between accounts.
3. **Manifest.** `display: standalone`, theme colour and background matching the green theme,
   icons at 192 and 512 px (plus a maskable icon), and shortcuts for Scan and Map.
4. **Install prompt.** Capture `beforeinstallprompt`, show an in-app banner, and call the prompt
   only on a user tap. Hide the banner after install or dismissal.
5. **Push subscription (opt-in).** The user taps "Remind me" to ask for notification permission.
   The client subscribes with the public VAPID key and sends the subscription to reward-service.
   Subscriptions are stored per user, with a unique key on the endpoint. Unsubscribe deletes the row.
6. **Re-engagement job (reward-service).** A scheduled job runs once a day at 10:00 Amsterdam
   time. It finds subscribed users with no daily activity for 7 days (the `daily_activity` table
   from ADR 0001), and sends one polite reminder at most every 7 days per user. The job uses a
   timeout and a per-user try/catch, so one failed push does not stop the rest.
7. **Sending pushes.** Requires a Web Push library for signing (VAPID and payload encryption).
   Hand-writing the crypto is not a good trade. This is a new backend dependency and needs an
   explicit decision (see below).

## Open decisions (needed before the push part starts)

1. **Push library.** Recommended: `nl.martinkoelewijn:webpush` or the `web-push` Java library. Pick
   one after the researcher checks maintenance status and licence.
2. **VAPID keys.** Generate a key pair once and set `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`,
   `VAPID_SUBJECT` as environment variables for reward-service and the frontend build. The public key
   may appear in the frontend. The private key must not.
3. **Icons.** The manifest needs PNG files. Who creates them: the team from an SVG source, or the
   owner supplies a logo?

## Consequences

- The offline cache can show stale collection points until the network returns. The UI should say
  when data is from the cache.
- Service worker updates can keep an old build in a user's browser until reload. Use
  `registerType: 'autoUpdate'` and document the behaviour.
- Push adds a scheduled job, so reward-service runs one new background task. Tests must not
  depend on the wall clock; the job takes a `Clock`.
