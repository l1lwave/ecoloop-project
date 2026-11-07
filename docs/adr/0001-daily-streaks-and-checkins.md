# ADR 0001: Daily streaks and check-ins in reward-service

Status: Proposed
Date: 2026-10-26

## Context

The `eco-family` badge promises "use the app 30 days in a row" but is only checked against a total
of 30 deposits (see `FUNCTIONALITY.md` §4.3). The app has no notion of daily activity, so users have
no reason to open it every day.

Login lives in `auth-service`, and `reward-service` never sees logins. Activity therefore has to be
reported to `reward-service` explicitly. Services integrate over HTTP and must not share tables, so
the activity record belongs in `eco_reward_db`.

## Decision

1. **Daily check-in endpoint.** `POST /api/rewards/checkin`, identity from the `X-User-Id` header
   only (gateway-verified JWT). The frontend calls it once per app open. The endpoint is idempotent
   per user per day: a second call on the same day returns the current streak and changes nothing.
2. **A day counts as active** if the user has a check-in or a deposit on that day. A deposit also
   counts as activity, so an active user is never told their streak broke because they used the
   scanner instead of the check-in.
3. **Day boundary** uses `Europe/Amsterdam` local date, injected through a `java.time.Clock` bean so
   tests can control time. The user base is Dutch; UTC midnight would break streaks for evening
   users.
4. **Storage.** Add a `daily_activity` table `(user_id, activity_date)` with a unique key on
   `(user_id, activity_date)`. The unique key makes concurrent check-ins safe: the second insert
   fails and is treated as "already checked in today". Add `current_streak`, `longest_streak`, and
   `last_active_date` to `user_stats`, updated in the same transaction as the insert.
5. **Streak rule.** If `last_active_date` is yesterday, `current_streak += 1`. If it is today, no
   change. Otherwise `current_streak = 1`. `longest_streak = max(longest_streak, current_streak)`.
6. **Badge.** Add `required_streak_days` to `badges`. `eco-family` changes from `required_deposits=30`
   to `required_streak_days=30`. Badge checks run on check-in and on deposit.
7. **Existing data.** Users who already hold `eco-family` keep it. Badges are never revoked by a
   rule change. Streak counts start at 0 for everyone; there is no backfill from deposit history,
   because past deposit dates would not prove daily app use.

## Consequences

- Schema changes rely on `ddl-auto: update`, which adds columns and tables but does not change or
  drop existing ones. `data.sql` uses `INSERT IGNORE`, so the `eco-family` change needs an explicit
  `UPDATE` statement. Changing the seed alone will not reach existing databases.
- No Flyway. Schema changes are not versioned, so the `UPDATE` must be idempotent.
- Streaks are an engagement feature only. They do not change points, so the leaderboard is
  unaffected.
- Rejected alternatives: a Redis or scheduled job to expire streaks (not needed; the streak is
  computed on the next activity, and the displayed value is reset to 0 on read if the last active
  day is older than yesterday); counting logins from `auth-service` (cross-service coupling).
