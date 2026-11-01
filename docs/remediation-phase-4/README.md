# Final remediation — Phase 4: unified row actions

Continues `1329c2bd3fdb84d9915c9bbafdf52ec4cfa047ab` on `fix/chms-final-remediation`. Scope is the detailed Phase 4 request supplied in chat; `ACTION column(1).txt` and `CHMS_REVIEWED_CHANGES.md` were not present in this checkout. Backend controller/service rules were inspected directly. No backend, database, DTO, endpoint, permission or calculation changes. No push or merge.

## Interaction pattern

`shared/ui/RowActions.tsx` provides typed callbacks for Details, optional Edit and contextual menu entries. Details uses an eye, Edit a pencil, and remaining actions a compact overflow menu with visible labels. Each icon has a tooltip, a record-specific accessible name and a 44 × 44px hit target. The cell contains at most three buttons, is consistently right-aligned and retains the existing table scroll behavior. There is no empty overflow button or duplicate Details/Edit menu entry.

Opening an icon/menu never submits a lifecycle mutation. Lifecycle/destructive menu choices retain confirmation dialogs; invoice Revise opens a review form explaining the new draft/version effect with an explicit **Create revision** submission. Existing form validation, mutation locks, errors, cache invalidation, success feedback and separate crane/customer activation controls remain intact.

Menus wait until their exit transition finishes before opening a dialog, preventing competing focus traps. Escape closes the menu and returns focus. Details/edit/confirmation closure restores the originating button; workflows fall back to the directory Refresh button when a successful action removes its trigger. Pending mutations disable row actions and confirmation/cancellation as before. Quotation/User soft deactivation and Operator/Timesheet permanent deletion use distinct, accurate confirmation wording.

## Exact action matrix

Every listed action is also constrained by full-record module access. No lookup permission grants full-record access.

| Module / state | Roles with access | Direct actions | Overflow actions |
| --- | --- | --- | --- |
| Cranes | ADMIN, SALES | Details, Edit | None; activation remains separate |
| Customers | ADMIN, SALES | Details, Edit | None; activation remains separate |
| Customers | OPERATIONS | Details | None; actual activation remains read-only |
| Job Cards PENDING | ADMIN, OPERATIONS | Details, Edit | Complete, Cancel |
| Job Cards PENDING | SALES | Details, Edit | Cancel only |
| Job Cards COMPLETED / CANCELED | ADMIN, SALES, OPERATIONS | Details | None |
| Timesheets, parent PENDING | ADMIN, OPERATIONS | Details, Edit | Delete |
| Timesheets, parent COMPLETED / CANCELED / missing / loading / failed | ADMIN, OPERATIONS | Details | None |
| Maintenance SCHEDULED | ADMIN, MAINTENANCE | Details, Edit | Start, Complete, Cancel |
| Maintenance IN_PROGRESS | ADMIN, MAINTENANCE | Details, Edit | Complete, Cancel |
| Maintenance COMPLETED / CANCELED | ADMIN, MAINTENANCE | Details, Edit | None |
| Maintenance all states | OPERATIONS | Details, Edit | None |
| Quotations | ADMIN, SALES | Details, Edit | Deactivate (soft) |
| Invoices DRAFT | ADMIN, FINANCE | Details, Edit | Complete, Cancel |
| Invoices COMPLETED | ADMIN, FINANCE | Details | Cancel, Revise |
| Invoices REVISED / CANCELED | ADMIN, FINANCE | Details | None |
| Users, non-ADMIN account | ADMIN | Details, Edit | Deactivate (soft) |
| Users, ADMIN account | ADMIN | Details, Edit | None, preserving existing admin protection |
| Operators | ADMIN | Details | Delete (permanent); no unsupported Edit |

FINANCE and MAINTENANCE receive no Customer Management access; all other excluded role/module combinations remain denied.

## Timesheet parent status and backend concerns

`TimesheetService.updateTimesheet()` and `deleteTimesheet()` both reject non-PENDING parents. The UI now reflects that existing rule. One shared React Query request to the existing `GET /api/job-card` supplies parent statuses for all visible timesheets; there are no per-row requests. It revalidates on entry and the Refresh control refreshes both lists. Unknown/missing parents and loading/errors fail closed for Edit/Delete while leaving Details accessible. Parent-query errors provide retry. Existing mutation invalidation refreshes the job list as well.

`MaintenanceService.updateMaintenance()` currently accepts terminal-state records, with date/crane validation and conditional scheduling checks. Terminal editing remains available for the existing authorized roles. Whether completed/canceled maintenance should become immutable is a future business/backend decision, not a new UI policy introduced here.

Concurrent changes can still occur after a directory fetch or dialog opens. The backend remains authoritative; a timesheet's parent becoming terminal produces the existing conflict feedback without hiding the row or discarding the dialog. Parent job lists are currently unpaginated; a future aggregate parent-status field or endpoint could reduce payload size for very large installations, but no backend expansion was needed for this phase.

## Changed files

- `frontend/src/shared/ui/RowActions.tsx`: reusable typed primary/overflow controls, accessible labels, targets, pending state and menu/dialog focus sequencing.
- `frontend/src/features/cranes/CranesPage.tsx` and `customers/CustomersPage.tsx`: replace duplicated Details/Edit menus with the shared controls; keep activation and existing focus fallback.
- `frontend/src/features/workflows/WorkflowPage.tsx`: shared actions, timesheet parent-state gating/query/retry, workflow focus restoration and explicit revision/delete review wording.
- `frontend/src/features/workflows/presentation.tsx`: remove the superseded workflow-only RowMenu implementation.
- `frontend/src/test/cranes.test.tsx` and `customers.test.tsx`: direct-button interactions and role/focus assertions; existing create/edit/details/activation coverage preserved.
- `frontend/src/test/workflows.test.tsx`: updated Details interactions, complete all-five-role lifecycle matrices, terminal maintenance edits, account/operator actions, one-query parent loading/error/retry, revision payload, and timesheet deletion confirmation/pending/conflict coverage.
- `frontend/src/test/rowActions.test.tsx`: keyboard traversal/activation, tooltip-backed accessible names, target sizing, overflow Escape/focus, no automatic action, unavailable controls and pending keyboard exclusion.
- `docs/remediation-phase-4/`: this record, synthetic browser results and screenshots.

## Verification

Linux / Node 24.19.0, November 1, 2026. Commands run from `frontend` using the existing dependencies and test configuration; no skipped tests or timeout/configuration changes were introduced.

| Check | Result |
| --- | --- |
| `npm run test:doctor` | Passed; expected branch/root/configuration |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run format:check` | Passed |
| `npm run test` | 302 passed, zero failures/skips; 9 files; 222.25 seconds (261 baseline tests plus 41 new cases) |
| `npm run build` | Passed; existing non-fatal Zod/Rollup annotation warnings |
| `git diff --check` | Passed |

The initial updated customer/crane/workflow suite passed 114 tests. New test-harness issues were corrected before final verification: the gated parent handler needed to invoke its gate callback, and a disabled-button test needed to verify keyboard exclusion rather than ask user-event to click a pointer-disabled MUI control. Existing assertions were adjusted only for the intended direct-button interaction and focus target. No tests were removed or disabled.

The role matrices exercise all five roles for jobs (three states), invoices (four states), maintenance (four states), timesheets (three actual parent states plus unknown parent), quotations, users and operators. Existing all-role customer/crane tests remain in place. Existing pending/conflict tests for job completion and operator deletion remain enabled; new tests cover timesheet pending/deletion conflict and explicit invoice revision payloads.

## Browser evidence and limitations

Actual Chromium with synthetic API fixtures, **not live Spring Boot/PostgreSQL acceptance**. Eight modules (Cranes, Customers, Job Cards, Timesheets, Maintenance, Quotations, Invoices, Users) were checked at 1440, 768 and 390px in both themes: 48 combinations. Checks include 44px touch targets, keyboard Details activation and return focus, no document overflow, and Job Card menu/confirmation keyboard focus without mutation. Terminal job/timesheet actions were checked. An additional mobile-dark Operator review passed Details, permanent-delete wording and cancellation-focus checks. Both browser runs recorded zero page/console errors and zero unintended mutations.

Evidence: `browser-results.json`, `browser-operators.json`, `actions-1440-light.png`, `actions-768-light.png`, `actions-390-dark.png` and `operator-confirmation-390-dark.png`. Fixture credentials are not real accounts. No real database was altered.

Accessible names, native button/menu semantics, keyboard focus and touch-capable Chromium were checked. Native screen-reader software, physical touch devices, Windows browsers and live-backend role/state races were not exercised. Backend sources are unchanged, so Maven/JaCoCo were not rerun. Live acceptance should confirm lifecycle operations against real records and concurrent terminal-parent conflicts. No later remediation phase is included.
