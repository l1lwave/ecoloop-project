# Plan: True streaks and daily check-ins

Status: Proposed. Decision record: [ADR 0001](../adr/0001-daily-streaks-and-checkins.md).

## Goal

Make `eco-family` ("use the app 30 days in a row") mean what it says, and give users a daily reason
to open the app.

## Scope

In:
- Daily check-in endpoint and a check-in on app open.
- Current and longest streak, stored and returned on the profile.
- `eco-family` unlocks on a 30-day streak instead of 30 deposits.
- Streak display on the profile page, plus i18n keys in all four locales.

Out (follow-ups):
- Streak push notifications or reminders.
- Streak-freeze or grace-day mechanics.
- Backfilling streaks from deposit history.

## Subtasks

| # | Subtask | Owner | Depends on |
|---|---|---|---|
| 1 | Schema: `daily_activity` table, `user_stats` streak columns, `badges.required_streak_days`, idempotent `UPDATE` for `eco-family` | senior-dev | — |
| 2 | `StreakService` with `Clock`-based day logic, unique-key idempotent check-in, unit tests for every public method | senior-dev | 1 |
| 3 | `POST /api/rewards/checkin` controller and DTO, Bean Validation, streak fields on `UserProfileResponse`, badge check on check-in and deposit | senior-dev | 2 |
| 4 | Check-in call on app open in `eco-frontend` (once per session/day, errors swallowed so the app still opens) | junior-frontend | 3 (API contract) |
| 5 | Streak display on `ProfilePage`, i18n keys in `nl`, `en`, `ua`, `ru` in the same change | junior-frontend | 3 |
| 6 | Tests: API tests for check-in and profile, frontend tests for the check-in call | junior-tester | 4, 5 committed |
| 7 | Review and final build | reviewer | 6 |

Subtask 2 has concurrency and transaction concerns (two check-ins at once, check-in racing a
deposit), so it goes to senior-dev, not a junior.

## API contract (for subtasks 3 and 4)

`POST /api/rewards/checkin`, header `X-User-Id` from the gateway only. No user ID in path or body.

Response 200:
```json
{ "currentStreak": 5, "longestStreak": 12, "checkedInToday": true, "newBadges": ["eco-family"] }
```

Repeated call on the same day: 200 with the same values and `newBadges: []`.

`GET /api/rewards/profile` adds `currentStreak` and `longestStreak` fields. Existing fields are
unchanged.

## Acceptance criteria

- Two check-ins on the same day create one activity row and do not change the streak.
- A check-in the day after the last activity adds 1 to the streak; after a gap of one or more days
  the streak restarts at 1.
- A deposit on a day counts as activity for that day.
- `eco-family` unlocks at a 30-day streak, not at 30 deposits. Users who already had it keep it.
- Both check-in and deposit paths use the Amsterdam date, verified in tests with a fixed `Clock`.
- No stack traces or internal details in error responses. The endpoint has rate limiting like the
  other endpoints.

## Open questions for the user

1. Should a deposit count as activity? The ADR assumes yes.
2. Should existing users who already hold `eco-family` keep it? The ADR assumes yes.

## Repo gaps found during planning

- `scripts/new-task.sh` and `scripts/merge-feature.sh` do not exist in the repo. Worktrees cannot be
  created through the documented process until they are added. Until then, the work runs on the
  current branch `integrate/2026-10-26` only after the owner agrees, and nothing is committed or
  merged by agents.
- `CLAUDE.md` says the Ukrainian locale may be `uk` or `ua`. The repo uses `ua.json`. The
  i18n change should follow `ua.json`.
- `develop` and `main` are referenced in `CLAUDE.md`, but the current branch is
  `integrate/2026-10-26`. Confirm the integration branch before subtasks start.
