# ADR 0003: Scan and return history

Status: Proposed
Date: 2026-10-27

## Context

Users want to see what they scanned, and what they actually returned for deposit, over time, with
charts. Two different things must be kept apart:

- **Scan event:** the user looked up a product. This happens for every product, including ones that
  cannot be returned. It lives in `scan-service`, because the lookup happens there.
- **Return (deposit record):** the user confirmed they returned a deposit item. It lives in
  `reward-service`, in `deposit_records`, which already has `deposited_at`.

The repo has no existing history implementation. `ScanController` has no user-facing history
endpoint, and `ScanPage` calls `identify()` and then `deposit()`.

Two gaps found while planning (must be decided before the return statistics are trusted):
1. The "Returned" button is shown when `result.recyclable` is true, not when the product is
   deposit-eligible. A recyclable non-beverage such as peanut butter can still be submitted, and
   `reward-service` credits points for any deposit.
2. `reward-service` does not know whether a deposit item is a beverage. It only receives
   `materialType` from the client.

## Decision

1. **Scan events in scan-service.** New table `scan_events` (id, user_id, barcode, product_name,
   material_code, deposit_value, scanned_at). A row is written on every successful `identify` call
   for an authenticated user. `user_id` comes from the gateway `X-User-Id` header only. Writes use
   JPA. The write is part of the identify request; if the write fails, the scan still returns and
   the failure is logged without personal data.
2. **Scan history endpoint.** `GET /api/scan/history?page=&size=`, newest first, `size` capped at 50.
   Returns barcode, product name, material label, recyclable flag, deposit value, scanned_at.
3. **Return statistics in reward-service.** `GET /api/rewards/returns/summary?year=YYYY` returns one
   entry per month with returned item count, deposit money (EUR), and plastic weight (kg). Monthly
   grouping uses the Amsterdam timezone, the same as the streak feature. Only `deposit_records` rows
   count as returns.
4. **Deposit value is decided server-side.** The reward side does not take a money amount from the
   client. The value is a fixed EUR 0.15 per returned item, which matches the scanner's current
   deposit value.
5. **Frontend.** A fourth "History" window on the profile page (reusing the existing tabs and swipe
   windows), with a paginated scan list and a Recharts chart of monthly returns and money. Recharts is
   the new frontend dependency; reason: it is React-native and needs no canvas setup. Pinned
   exact version, at least two weeks old.

## Open decision (blocks the return statistics)

How does `reward-service` know that a return is a real deposit item?

- **A (recommended):** `POST /api/rewards/deposit` calls `scan-service` over HTTP to check the
  barcode's deposit value, and only credits when it is greater than zero. The barcode is
  validated before the call, and the call has a timeout.
- **B:** the client sends `materialType` as today, and the Returned button is shown only when the
  scan result has `depositValue > 0`. Simpler, but a modified client can still submit any item.

## Consequences

- Two services each own their data. The history views combine nothing across databases.
- Scan events grow with every scan. The history endpoint is paginated. Retention is unlimited for
  now; a retention rule is a follow-up.
- Money figures are estimates from a fixed value, not a payment. The UI must say that they are
  deposit values, not payouts.
