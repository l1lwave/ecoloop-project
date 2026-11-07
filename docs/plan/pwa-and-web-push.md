# Plan: PWA offline support and Web Push re-engagement

Status: Proposed. Decision record: [ADR 0004](../adr/0004-pwa-and-web-push.md).

## Goal

EcoLoop installs on phones and desktops, works offline for collection points and map tiles, and can
send a polite reminder to users who have been inactive for 7 days.

## Phases

Phase 1 (can start now): PWA, offline cache, install banner. No backend change, no secrets.

Phase 2 (blocked on three decisions in ADR 0004): push subscription, VAPID configuration, the
re-engagement job in reward-service.

## Subtasks

| # | Subtask | Owner | Phase | Depends on |
|---|---|---|---|---|
| 1 | Add `vite-plugin-pwa` (pinned), manifest, icons, Workbox config with the cache rules in ADR 0004 | junior-frontend | 1 | — |
| 2 | Install banner component with `beforeinstallprompt`, i18n keys in nl/en/ua | junior-frontend | 1 | 1 |
| 3 | Verify the user-specific API routes are not cached (test or config check) | junior-tester | 1 | 1 |
| 4 | Push library choice and VAPID setup | owner decision, researcher check | 2 | — |
| 5 | Subscription endpoint and table in reward-service, opt-in UI, unsubscribe | senior-dev (storage and consent) and junior-frontend (UI) | 2 | 4 |
| 6 | Scheduled re-engagement job with `Clock`, per-user try/catch, 7-day throttle | senior-dev | 2 | 5 |
| 7 | Review and final build | reviewer | both | all |

Subtask 6 sends messages to real devices and uses personal data, so it goes to senior-dev, with the
junior building the parts around it.

## Acceptance criteria (Phase 1)

- The app shell and UI routes load offline after one visit.
- The last collection-point list loads offline, marked as cached.
- Map tiles already viewed load offline, within the cache limits.
- No response that depends on the signed-in user is ever stored by the service worker.
- The manifest passes the install criteria in Chrome's Lighthouse PWA check.
- The install banner appears only when the browser offers installation, and the install prompt is
  called only from a user tap.
- All new strings are in nl, en and ua. ru remains a known gap.

## Acceptance criteria (Phase 2)

- Notification permission is requested only after the user taps "Remind me".
- The subscription is stored only after the user opts in, and deleted on unsubscribe.
- The job sends at most one reminder per user per 7 days.
- A failed push to one user does not stop the job.
- No VAPID private key appears in any tracked file.

## Open decisions for the owner

1. Push library: approve the researcher's pick (ADR 0004, decision 1), or name one.
2. VAPID keys: will you generate and set them in IntelliJ, or should we add a documented script?
3. Icons: do you have a logo file, or should the team create one?
