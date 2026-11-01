# Final remediation — Phase 3: Reports UX and presentation

Based on verified Phase 2 commit `9832f6922818fe3200a85b7b9f22a2e0640dffe5` on `fix/chms-final-remediation`. Implements the supplied Phase 3 request only. Backend source, calculations, DTOs, permissions, endpoints and database are unchanged. No push or merge is part of this phase.

## Implemented behavior

- The Reports page retains its single primary heading and replaces the redundant Report builder heading/single dropdown with dependent **Report Category** and **Report Type** selectors. Options come from the existing 18 contracts, filtered by the signed-in role.
- Category/type changes clear filters, results, errors and pagination. Editing a filter also clears the applied request; only **Run report** applies valid inputs. Query cancellation uses the React Query abort signal through Axios. A late response cannot appear under a different report. Running the same applied request explicitly refreshes it.
- Date inputs give immediate feedback for partial or reversed ranges. Invoice Date and Revenue Summary Date require both values; general Job Cards permits both empty or a complete pair. Equal dates are valid. Job Cards labels explicitly say **Job Date From/To**, with a note that the existing service filters `jobDate`, not scheduled/work dates. Requests keep their original `fromDate`/`toDate` query parameters.
- Relationship lookups keep named records and stable numeric IDs. Active ADMIN entries returned by the salesperson lookup remain selectable; historical positive-ID entry remains available. This preserves reporting on admin-created jobs and records absent from active-only lookups. No lookup eligibility or permissions changed.
- Typed presentation configurations use actual Java report DTO keys. Fixed headers remain visible for initial, loading and empty results. Dates, counts, amounts and statuses use existing formatters; no currency, tax, percentage or derived financial metric was added. Text wraps within readable column widths; wider tables scroll inside a focusable region.
- Overall and date-filtered revenue summaries show all four response values once in a compact table: Total Invoices, Total Amount, Total Mobilization and Total Revenue. The duplicate KPI cards and irrelevant summary pagination are removed. No financial values are recomputed.
- Job Cards columns are Job Card Number, Crane Number, Customer Name, Location, Start Date, End Date, Sales Person, Operator, Status and Total Worked Hours. The four filtered variants omit their respective Customer Name, Crane Number, Status or Sales Person column. `operatorName` supplies Operator; `jobDate` and `totalBillableHours` remain in responses but are hidden.
- Invoice columns are Invoice Date, Invoice Number, Job Card Number, Crane Number, Customer Name, Description and Amount. By-customer omits Customer Name. Qty, Rate, Mobilization and Status stay out of these report tables. Descriptions preserve line breaks and wrap.
- Crane utilization retains crane number/type, total jobs and total worked hours. Revenue Monthly/Customer/Crane retain every returned field, including month/year and customer/crane identities.
- Maintenance columns are Maintenance Number, Crane Number, Crane Type, Repair Type, Start Date, End Date, Maintenance Person and Status. `repairType` supplies Repair Type; `maintenanceDate` is hidden. Missing assignees/end dates render as a dash. Maintenance Summary hides `craneId` but retains identity, record count and days. The status report is labeled **Maintenance · Status**.

## Preserved API mappings and access

All paths below are GET paths under `/api/reports`. A = ADMIN, S = SALES, O = OPERATIONS, F = FINANCE, M = MAINTENANCE. The access matrix was checked against `ReportController`; frontend tests exercise the 57 permitted report/role combinations using MSW, not a live security server.

| Category | Type | Path | Filters | Roles |
| --- | --- | --- | --- | --- |
| Job Cards | General | `/job-cards` | Optional complete date pair | A S O F |
| Job Cards | Customer | `/job-cards/customer/{customerId}` | Customer | A S O F |
| Job Cards | Crane | `/job-cards/crane/{craneId}` | Crane | A S O F |
| Job Cards | Status | `/job-cards/status/{status}` | Job status | A S O F |
| Job Cards | Sales Person | `/job-cards/sales-person/{salesPersonId}` | Sales person, including eligible admins | A S O F |
| Invoices | General | `/invoices` | None | A F |
| Invoices | Date | `/invoices/date` | Required date pair | A F |
| Invoices | Customer | `/invoices/customer/{customerId}` | Customer | A F |
| Invoices | Status | `/invoices/status/{status}` | Invoice status | A F |
| Crane | Utilization | `/crane-utilization` | None | A S O |
| Revenue Summary | Overall | `/revenue-summary` | None | A S O F M |
| Revenue Summary | Date | `/revenue-summary/date` | Required date pair | A S F |
| Revenue | Monthly | `/revenue/monthly` | None | A S F |
| Revenue | Customer | `/revenue/customer` | None | A S F |
| Revenue | Crane | `/revenue/crane` | None | A S F |
| Maintenance | General | `/maintenance` | None | A O M |
| Maintenance | Summary | `/maintenance/summary` | None | A O M |
| Maintenance | Status | `/maintenance/status` | None | A O M |

## Files changed

| File | Purpose |
| --- | --- |
| `frontend/src/features/workflows/ReportsPage.tsx` | Dependent selectors, filter validation/display, applied-request isolation, reset/cancellation/pagination behavior |
| `frontend/src/features/workflows/reports.ts` | Category/type metadata for the same contracts; status options, filter validation and request URL construction |
| `frontend/src/features/workflows/reportPresentation.ts` | Typed Java-DTO presentation shapes and explicit ordered columns for every report variant |
| `frontend/src/features/workflows/ReportResults.tsx` | Shared table rendering, compact object summaries, formatting and empty/loading states |
| `frontend/src/test/reports.test.tsx` | 83 new report regressions |
| `frontend/src/test/workflows.test.tsx` | Two existing report tests updated for the intentional selector labels and removal of duplicate summary cards |
| `docs/remediation-phase-3/README.md` | Scope, contracts, verification and remaining acceptance work |
| `docs/remediation-phase-3/browser-results.json` | Results of 24 fixture-backed browser layout checks plus date/keyboard checks |
| `docs/remediation-phase-3/*.png` | Six representative screenshots: desktop jobs in both themes, tablet invoices, mobile maintenance, desktop/mobile summary |

## Verification

Executed in Linux / Node 24.19.0 with the existing dependencies and Vitest configuration; no timeout or test configuration changes. Commands run from `frontend`:

| Check | Result |
| --- | --- |
| `npm run test:doctor` | Passed; correct branch, frontend root/config, single worker, 15000ms timeout |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run format:check` | Passed |
| `npm run test` | 261 passed, zero failed/skipped; 8 files; 170.79 seconds |
| `npm run test -- src/test/reports.test.tsx -t 'renders actual DTO'` | 9 affected rendering cases passed after the final text-width polish; 74 other cases intentionally excluded by the test-name filter |
| `npm run build` | Passed; existing non-fatal Zod/Rollup annotation warnings |
| `git diff --check` (repository root) | Passed |

The regression coverage includes all 18 endpoint mappings across all permitted roles (57 cases), five role-specific option lists excluding unauthorized selections, nine populated DTO layouts with deliberately reordered response keys, all three date-range forms, empty lists, object summaries, required/invalid status and relation inputs, admin lookup selection, historical-ID entry, stale request cancellation, pagination/filter resets, retryable errors, 403 access denial, 401 session expiry and anonymous navigation. All 178 existing workflow/auth/customer/crane/UI tests remain enabled and pass, with 83 additional report tests. TypeScript, lint, format and production build checks also passed again after the final text-width polish.

During initial verification, two new test-harness issues were corrected: reopening the autocomplete after invalid input, and checking the actual Axios request abort signal instead of an MSW synthetic request's signal. Both focused cases passed afterward. An unsupported RTL `exact` role-query option was removed during TypeScript verification. The browser harness was adjusted for MUI's accessible names, which include the selected option in real Chromium. No assertions or existing test coverage were removed.

## Browser evidence and remaining acceptance

Chromium/Playwright ran the actual frontend with **synthetic API fixtures**, not a live Spring Boot/PostgreSQL session. Job Cards, Invoices, Maintenance and Revenue Summary were checked at 1440, 768 and 390px in both light and dark themes (24 combinations). Each retained one primary heading, no document horizontal overflow, and a focusable table scroll region. Browser checks also exercised incomplete/reversed dates, same-day request parameters, keyboard selection of an admin salesperson and the Maintenance · Status label. Final results: zero page exceptions, console errors or failed requests. A narrow mobile text column found during review was corrected with a minimum text width and checked again.

Representative evidence:

- [Job Cards — desktop light](jobs-1440-light.png) and [dark](jobs-1440-dark.png)
- [Invoices — tablet](invoices-768-light.png)
- [Maintenance — mobile](maintenance-390-light.png)
- [Revenue Summary — desktop light](revenue-summary-1440-light.png) and [mobile dark](revenue-summary-390-dark.png)

Backend sources did not change, so Maven/JaCoCo were not rerun for this frontend-only phase. This work does not claim new live-backend financial reconciliation, server-side authorization testing, native Windows testing or exhaustive screen-reader coverage. Report list pagination remains client-side because the existing endpoints return complete lists. Active-only lookup queries still omit inactive records; the historical-ID fallback is retained.

Before production acceptance, run the reports against the real backend as each relevant role; compare report figures with known completed invoices (including nonzero mobilization), confirm historical admin-created jobs appear when filtered, and check jobDate boundaries and unassigned maintenance against saved records. No report calculation or deployment policy was changed to make presentation pass.
