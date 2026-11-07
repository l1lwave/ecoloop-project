# ADR 0002: Referral system in reward-service

Status: Proposed
Date: 2026-10-26

## Context

Users should be able to invite friends with a link or QR code and earn bonus points or an
achievement. The main risk is abuse: someone creating many accounts to farm points.

Registration lives in `auth-service`. Services integrate over HTTP and must not share tables, so
referral data belongs in `eco_reward_db`, and the rewards logic stays in `reward-service`.

## Decision

1. **Referral code.** Each user gets one code, generated on first request with a random
   8-character alphanumeric value (no ambiguous characters). Stored in a `referral_codes` table
   with a unique key on `code` and on `user_id`. The code is not derived from the user ID.
2. **Claiming.** The invited user calls `POST /api/rewards/referrals/claim` with `{ "code": "..." }`
   after registering or signing in. This creates one pending referral row. Rules:
   - The code must exist.
   - A user cannot claim their own code.
   - A user can claim only once, enforced by a unique key on the invited user.
   - Claims are accepted only while the invited user has no deposits.
3. **Payout.** Bonus points are credited when the invited user makes their first deposit. The
   referrer and the invitee each get 50 points. The payout and the status change happen in the same
   transaction, and the referral row is marked credited so it can't pay twice.
4. **Achievement.** The referrer gets the `eco-ambassador` badge after 3 credited referrals. This
   uses the existing badge mechanism: a new `required_referrals` column on `badges`, checked in
   `BadgeService` as the streak check is.
5. **Link and QR.** The frontend builds the link `<origin>/register?ref=<code>` and renders it as a
   QR code with the `qrcode` npm package. The backend only returns the code, not an image.
   The frontend reads `ref` from the URL and sends it to the claim endpoint after sign-in. It is
   not stored in localStorage.
6. **Privacy.** A referrer sees only the count of credited referrals, not who was invited.
7. **Rate limiting.** The claim endpoint uses the existing gateway rate limit. Invalid codes return
   the same error as unknown codes, so codes cannot be probed for existence.

## Consequences

- No cross-service call. `auth-service` is unchanged.
- Points are paid after a deposit, so a fake account costs real effort. Abuse is limited, not
  eliminated: one person can still create accounts and deposit. Fraud beyond that is out of scope.
- Schema changes use `ddl-auto: update`. New tables are created automatically. The new badge is
  seeded with an idempotent statement, following the pattern in ADR 0001.
- Rejected: crediting at sign-up (easy to farm); a separate referral service (not needed for this
  size).
