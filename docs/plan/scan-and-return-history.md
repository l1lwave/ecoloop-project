# Plan: Scan and return history

Status: Proposed, blocked on one decision (see ADR 0003, "Open decision").
Decision record: [ADR 0003](../adr/0003-scan-and-return-history.md).

## Goal

Users can see every product they scanned, and every item they returned, over time, with monthly
charts of returns, deposit money, and plastic weight.

## Scope

In:
- `scan_events` table and recording on every successful identify (scan-service).
- Paginated scan history endpoint (scan-service).
- Monthly return summary endpoint (reward-service).
- "History" window on the profile page: scan list plus a Recharts chart.
- i18n in `nl`, `en`, `ua`. `ru` remains a known gap.

Out (follow-ups):
- Retention or deletion of old scan events.
- Exporting history as a file.
- Real payouts or money transfer.

## Subtasks

| # | Subtask | Owner | Depends on |
|---|---|---|---|
| 1 | scan-service: `scan_events` entity and repository; record event in `identify` with the gateway user header; tests | junior-backend | — |
| 2 | scan-service: `GET /api/scan/history` with paging (size cap 50), newest first; tests | junior-backend | 1 |
| 3 | reward-service: `GET /api/rewards/returns/summary?year=`, monthly aggregation in Amsterdam time, EUR 0.15 per item; tests | junior-backend, senior-dev review | open decision |
| 4 | Deposit gating (open decision A or B) | senior-dev | open decision |
| 5 | Frontend: History window, scan list with load more, Recharts chart, i18n | junior-frontend | 2, 3 contracts |
| 6 | Tests and review | junior-tester, reviewer | 1–5 committed |

Subtask 4 touches money and cross-service trust, so it goes to senior-dev. Subtask 3 is an
aggregation with timezone rules. The junior does the query, and senior-dev reviews the money path.

## API contract

`GET /api/scan/history?page=0&size=20` (identity from `X-User-Id` only):
```json
{ "items": [ { "barcode": "...", "productName": "...", "materialLabel": "PET",
               "recyclable": true, "depositValue": 0.15, "scannedAt": "2026-10-27T10:00:00" } ],
  "page": 0, "size": 20, "hasMore": true }
```

`GET /api/rewards/returns/summary?year=2026` (identity from `X-User-Id` only):
```json
{ "year": 2026,
  "months": [ { "month": 1, "itemsReturned": 4, "depositEur": 0.60, "plasticKg": 0.12 } ],
  "totalItems": 4, "totalDepositEur": 0.60, "totalPlasticKg": 0.12 }
```
Months with no returns are included with zeros, so the chart has 12 points.

## Acceptance criteria

- A successful identify for a logged-in user writes exactly one scan event. A failed identify writes
  none.
- The history list is newest first, pages stop at `hasMore: false`, and `size` above 50 is capped.
- Each user sees only their own scans and returns. The user ID is never taken from the request.
- The summary uses Amsterdam month boundaries and includes 12 months.
- Deposit money is computed server-side as EUR 0.15 per returned item. No client value is trusted.
- The UI labels money as "deposit value", not as a payout.
- Error responses contain no stack traces.

## Dependencies on earlier open items

- Peanut butter and similar items can still reach the Returned button, because it is shown for
  recyclable items. Subtask 4 closes this. The return statistics must not count such items until
  then.
