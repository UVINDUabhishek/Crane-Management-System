# Final remediation — Phase 1

Scope: the Phase 1 requirements supplied in the task message. A separate `CHMS_REVIEWED_CHANGES.md` attachment was not present in the workspace. No later phases implemented.

Branch: `fix/chms-final-remediation`, created from `feat/premium-enterprise-ux` at `7f4a794dbaa5f54b8002df2aab47d166dfa4cf93`. Original branch and stashes preserved. No push or merge.

## Changes

- Overview, Cranes and Customers use the compact shared page heading without redundant eyebrow/subtitle spacing. Cranes/Customers refresh actions sit beside directory search; the existing focus fallback ref remains attached.
- Shared FilterToolbar keeps a compact reset button in place, disabled when there is nothing to clear. Filter chips wrap in the remaining space. Job/invoice/maintenance status toolbars reuse it, preventing reset-control appearance from shifting the table downward.
- Crane directory omits Capacity, Manufacturer and Year; details and forms retain them. Crane details omit numeric ID. Customer directory omits Registration Number; forms/details and registration search remain intact.
- Job directory omits Operator; details retain its readable name. Job, timesheet, maintenance, quotation and user details omit internal numeric IDs while preserving human-readable references. Description occupies a full-width, wrapping area. User dialog title is exactly `User details`.
- Quotation column heading is `Valid Period`; its action remains `Deactivate` with retention guidance.
- Invoice details are a dedicated read-only document: number/date/status/version, customer/crane/job/quotation references, quantity/rate/mobilization, server-provided total and description. No invented address, tax, currency or computed total. Version 1 displays `Original`; later versions display `Revision N` independently of lifecycle status, both in the directory and details.
- Operator action is `Delete` with named confirmation, irreversible-deletion guidance and normalized API conflict handling. The backend's hard-delete endpoint, authorization and existing mutation/error behavior are preserved.
- Existing summary cards remain unchanged, including all four cards for jobs, maintenance and invoices and the timesheet total.

No API response fields were removed; contracts, DTOs, database, backend, permissions, form requests and lifecycle endpoints remain unchanged. `contracts.ts` changes only the operator action's display label.

## Verification

Executed November 1, 2026 (Asia/Colombo), on Linux with Node 24:

| Command | Result |
| --- | --- |
| `npm run test:doctor` | Passed; expected frontend configuration and 15000 ms timeout |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run format:check` | Passed |
| `npm run test` | 162 passed, zero failed/skipped; six files; 83.27 seconds |
| `npm run build` | Passed; existing non-fatal Zod annotation warnings |

The focused workflow run also passed all 29 tests before the final two regression cases were added. Final complete verification includes those cases. The original 149 tests remain, with 13 additional regression cases.

Regression coverage adds hidden-column/detail-ID checks, readable references, all three invoice version labels, actual invoice values, disabled reset state, quotation deactivation wording, and operator deletion cancellation/conflict/success refresh. All baseline tests remain.

## Browser evidence

System Chromium with Playwright, using synthetic intercepted API responses only. No production/developer data modified; this is presentation verification, not live backend acceptance.

- 18 reset-layout checks: Cranes, Customers and Job Cards at 1440/768/390px in both light and dark themes. Table top position stays unchanged on filter/reset. One primary page heading and no document-level horizontal overflow.
- Six invoice detail checks across the same widths/themes; no dialog horizontal overflow. Read-only content remains scrollable on smaller displays.
- No JavaScript page exceptions.
- `browser-results.json` records results. `cranes-*.png` and `invoice-*.png` provide representative visual evidence.

The first browser attempt measured the previous route during navigation; waiting for the destination heading resolved the harness race. A formatting invocation initially used `contracts.tsx` instead of `contracts.ts`; the correct file and complete format check were subsequently verified.

## Limits

Native Windows and full screen-reader acceptance were not executed here. Backend verification was not rerun because this phase changes only frontend presentation and tests. Existing small-screen tables retain contained horizontal scrolling; no currency or unsupported invoice data was introduced. Operator relationship errors remain server-authoritative and are shown in the confirmation dialog without falsely removing the record.
