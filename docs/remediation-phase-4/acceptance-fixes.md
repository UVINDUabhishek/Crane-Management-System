# Recovered acceptance fixes

Review branch: `review/chms-acceptance-fixes`, created from the verified remote remediation commit `d25092af583cbab94c2377493180fcdf2a1a1d69`. The old `work` checkout was preserved. Only this review branch is intended for publication; remediation/main are not modified or merged.

## Timesheet actions

`WorkflowPage` previously included `!parents.isFetching` in action eligibility. A parent-list refetch therefore removed the overflow trigger even with a known-PENDING parent, detaching the confirmation dialog's saved focus target. Eligibility now retains that known-PENDING control while `RowActions.changesDisabled` disables Edit, overflow and menu actions during revalidation. Details remains available. Unknown/loading-without-data, failed and terminal parents still cannot expose Edit/Delete. The existing shared job query and mutation locks/conflict handling are preserved.

The deletion regression holds a parent refetch open using an MSW request gate. It checks that the original trigger stays mounted and disabled and Details stays enabled, then releases the query and verifies cancellation focus, duplicate prevention and server lifecycle-conflict feedback. Restoring the old rendering condition made this regression fail; restoring the correction made it pass. No sleep, timeout increase or removed assertion was used.

## Maintenance policy

This supersedes the original Phase 4 decision to retain terminal editing. Only SCHEDULED and IN_PROGRESS are editable. `MaintenanceService.updateMaintenance()` rejects COMPLETED/CANCELED with the existing HTTP 409 conflict mechanism immediately after the locked record lookup, before validation, scheduling calls, relationship changes or persistence. Existing lifecycle and crane-availability logic remains unchanged.

The frontend contract restricts Edit to those same states under existing RBAC. Details remains available. A regression verifies a concurrent 409 leaves the edit dialog, server message and entered description intact.

Service tests cover both permitted states and both rejected terminal states, verifying no repository save or scheduling/crane/user interaction for rejected updates. PostgreSQL integration tests exercise all four states through REST and verify HTTP status, description persistence, record status, crane assignment and crane availability.

## Changed files

- `frontend/src/features/workflows/WorkflowPage.tsx`: stable known-PENDING action eligibility and refetch disabling.
- `frontend/src/shared/ui/RowActions.tsx`: separately disable write controls without disabling Details.
- `frontend/src/features/workflows/contracts.ts`: maintenance edit-state restriction.
- `frontend/src/test/workflows.test.tsx`: controlled refetch/focus regression, all-role terminal maintenance matrix and concurrent conflict retention.
- `src/main/java/com/uvindu/crane_management_system/service/MaintenanceService.java`: early terminal-state rejection.
- `src/test/java/com/uvindu/crane_management_system/service/MaintenanceServiceTest.java`: permitted/rejected state tests and no-side-effect assertions.
- `src/test/java/com/uvindu/crane_management_system/integration/MaintenanceIntegrationTest.java`: four-state REST/persistence regression.
- This document and the historical Phase 4 README's supersession notice.

## Recovery differences and limitations

The unavailable workspace was not reconstructed wholesale. These changes were independently reapplied to the exact verified baseline from the supplied behavior and prior conversation. The integration fixture changes its known description directly instead of using the earlier regex replacement, and now explicitly asserts that the crane assignment is preserved. There is no intended behavioral difference from the two requested corrections.

Verification uses Linux/Node 24 and Java 21. Backend integration tests use PostgreSQL Testcontainers; the plain application-context test uses a separate disposable PostgreSQL instance on port 55439. No real application database or credentials are used. Windows-specific timing, physical devices, native screen readers and live-browser acceptance have not been reproduced in this recovery task. The controlled refetch reproduces the identified rendering defect rather than claiming to recreate the entire earlier Windows environment.

The first npm installation attempt failed with its default cache configuration; using the existing writable `/workspace/.npm` cache succeeded without dependency/lockfile changes. Build output retains existing non-fatal dependency annotation warnings. No generated logs, patches, archives, build outputs or credentials belong in the review commit.

## Verification results

Executed on November 2, 2026 (Asia/Colombo):

| Verification | Actual result |
| --- | --- |
| Controlled negative test with original rendering condition | Failed as expected; corrected source restored afterward |
| Focused refetch/focus and concurrent conflict regressions | 2 passed; other tests excluded by the explicit name filter |
| Full `npm run test` | 303 passed, zero failed/skipped, 9 files, 108.06 seconds |
| `npm run test:doctor` | Passed; correct review branch/configuration |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run format:check` | Passed |
| `npm run build` | Passed |
| Maven wrapper `clean verify` | BUILD SUCCESS; 562 tests, zero failures/errors/skips; 1 minute 15 seconds |
| JaCoCo | All existing thresholds passed; 47 classes analyzed |
| Diff/whitespace review | Passed; no unrelated source/configuration changes |

The disposable application-context database was stopped after verification. No unresolved verification failures remain; native Windows/live-browser acceptance limitations above still apply.
