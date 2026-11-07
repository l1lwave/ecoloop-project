# Plan: Referral system (invite a friend)

Status: Proposed. Decision record: [ADR 0002](../adr/0002-referral-system.md).

## Goal

Users invite friends with a link or QR code. The friend gets a bonus once they make a first deposit,
and the inviter earns bonus points and an achievement.

## Scope

In:
- Referral code per user, a claim endpoint, payout on first deposit.
- `eco-ambassador` badge after 3 credited referrals.
- Frontend: a "Invite a friend" section on the profile page, showing the link and a QR code,
  copy-to-clipboard, and the referral count. Claim on sign-in when `?ref=` is present.
- i18n keys in `nl`, `en`, `ua`. `ru` remains a known gap, as before.

Out (follow-ups):
- Sharing via native share sheets or social apps.
- Multi-level or recurring referral rewards.
- Fraud detection beyond the first-deposit rule.

## Subtasks

| # | Subtask | Owner | Depends on |
|---|---|---|---|
| 1 | Schema: `referral_codes`, `referrals` tables; `badges.required_referrals`; idempotent seed for `eco-ambassador` | senior-dev | — |
| 2 | `ReferralService`: get-or-create code, claim rules, payout on first deposit in the same transaction as the deposit, unit tests for every public method | senior-dev | 1 |
| 3 | Controller and DTOs: `GET /api/rewards/referrals/me`, `POST /api/rewards/referrals/claim`; hook payout into `RewardService.processDeposit`; badge check | senior-dev | 2 |
| 4 | Frontend: invite section on ProfilePage with link, QR (`qrcode` package), copy button; claim on sign-in from `?ref=` | junior-frontend | 3 contract |
| 5 | Tests: API and service tests; frontend tests if a framework exists (currently none) | junior-tester | 3, 4 committed |
| 6 | Review and final build | reviewer | 5 |

Subtasks 2 and 3 handle transactions and points, so they go to senior-dev. The frontend is kept
separate so it can run in parallel with the backend, using the contract below.

## API contract (subtasks 3 and 4)

All endpoints take identity from the gateway `X-User-Id` header only.

`GET /api/rewards/referrals/me` returns 200:
```json
{ "code": "K7M2QX9P", "creditedCount": 2, "pendingCount": 1, "ambassadorUnlocked": false }
```
The badge flag is `ambassadorUnlocked` (camelCase, as the other DTOs use).

`POST /api/rewards/referrals/claim` with body `{ "code": "K7M2QX9P" }`:
- 200 `{ "claimed": true }` on success.
- 400 `{ "error": "INVALID_REFERRAL", "message": "Referral could not be claimed" }` for an unknown
  code, self-referral, a repeat claim, or a user who already has deposits. Same body in all cases.
- Code validated with Bean Validation (format and length) before any lookup.

`POST /api/rewards/deposit` response is unchanged, but `newBadges` can now include
`eco-ambassador` for the referrer.

## Acceptance criteria

- A user always gets the same code on repeated calls.
- A claim creates one pending referral. No points are paid at claim time.
- The first deposit of the invited user credits 50 points to both users, once.
- The third credited referral unlocks `eco-ambassador` for the referrer. Badges are never revoked.
- Self-referral, repeat claim, unknown code, and claim after a deposit are all rejected with the
  same generic error.
- The QR code and link encode the same code and URL.
- The referrer never sees the invitee's user ID.
- Error responses contain no stack traces or internal detail.

## Open choices (made as defaults; change if you disagree)

1. Bonus: 50 points each side, on the invitee's first deposit.
2. Achievement: `eco-ambassador` after 3 credited referrals.
3. Claim allowed only before the invitee's first deposit.
