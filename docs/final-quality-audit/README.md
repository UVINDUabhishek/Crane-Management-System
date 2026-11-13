# CHMS quality audit and acceptance status

The latest completed verification is **562 backend tests and 316 frontend tests passed**, with JaCoCo thresholds, frontend typechecking, lint, formatting and production build passing. The [visual acceptance report](visual-acceptance-review.md) records actual Chromium checks at 375, 768, 1280 and 1440px, plus explicit remaining limitations.

See the [portfolio readiness review](portfolio-readiness.md) for the final Git, documentation, ignore-rule and credential-pattern checks. The original task request remains local; it is not a portfolio deliverable.

## Earlier remediation record (historical)

The record below predates the successful backend run and visual acceptance pass. Its Docker blocker, 312-test count and statements that browser inspection was outstanding have been superseded by the results linked above. They are retained as history, not current limitations.

## Baseline and safety

Authorized branch: `checkpoint/chms-local-20261111`; HEAD `6f85c172fdbe2f96e6b61fcbee605e028bd65d68`. Root report read in full. Eight staged file modifications and the untracked report were present at start and preserved. No branch operations, commits, pushes or database migrations performed. This verifies the previously observed local modifications, not the existence of unknown changes in other workspaces.

## Findings and corrections

- RowActions right justification shifted View when optional controls were absent. Reserve three 44px grid slots with a noninteractive empty Edit slot. All directories already share RowActions.
- Workflow routes reused the same component identity, allowing search/status/page and dialog state to leak. Key workflow elements by module; navigation regression verifies deliberate reset on module change.
- Metric cards lacked consistent hover feedback. Add subtle teal border/shadow without changing dimensions, semantics or motion preferences.
- Popup menus lacked a unified outline. Scope theme borders and shadows to Menu and Autocomplete paper; do not affect dialogs or cards.
- Timesheet Job Card options concatenated reference/customer text. Render separate columns using existing jobCardNumber/customer metadata, stable option IDs, searchable labels and customer truncation/title.
- Sidebar promotional copy removed; original SVG crane mark and matching favicon added. Refined navy background; native scrollbar CSS handles light/dark and forced-color fallback.
- Combined Timesheet cancellation/focus/delete-conflict regression split into independent tests. Request-gate start expectations now bounded and gates are released at teardown even after assertion failures; no global timeout increase or removed assertions.
- A menu action queued before exit could run after eligibility changed. Recheck current disabled state and trigger connection before executing the queued action.
- An open Timesheet confirmation could submit during parent revalidation. Disable confirmation and guard its handler during revalidation or missing/terminal/failed parent eligibility; Cancel remains available when no mutation is pending.
- Refresh remains ARIA-disabled and focusable while fetching, with activation guarded. Dialog exit now supports fallback focus when the original trigger is removed or natively disabled. ARIA-disabled triggers remain suitable focus targets while activation is blocked.
- Additional significant test isolation defect: application context smoke test used normal datasource properties. Import existing Testcontainers configuration so verification cannot initialize the real application database.

## Existing behavior preserved

MaintenanceService locks the record and rejects COMPLETED/CANCELED before field changes, scheduling calls or save. Existing service tests verify no side effects; PostgreSQL integration verification requires Docker. Failed RecordForm mutations retain React Hook Form values, keep the dialog open and expose normalized server conflicts. Pending mutation guards, role/state action matrices, job completion, invoice contracts, scheduling rules and backend authorization remain intact. No speculative changes to assignment role policy or financial calculations.

## Verification

- Backend `./mvnw.cmd clean verify`: 562 tests, 300 passed, 0 assertion failures, 262 errors, 0 skipped. Repeated outside sandbox: same totals. Docker Desktop Linux engine named pipe is absent (`docker info` fails). JaCoCo verification was not reached. Tests were not disabled or redirected to a real database.
- Frontend focused row-action/relation run: 20/20 passed before the last regression additions.
- Final targeted regression run: 7 passed, 88 unselected by test-name filter, 0 failed (3 files). These are filter exclusions, not disabled tests.
- TypeScript, ESLint, Prettier, production Vite build and test:doctor passed. Build emitted dependency-only Zod annotation warnings.
- Final complete frontend suite: **312 passed / 312**, 9 files, 0 failed, 0 skipped, 511.66 seconds (`npm run test -- --reporter=verbose`).
- Intermediate runs exposed an option accessible-name mismatch and a combined navigation regression timeout. Preserve the existing option accessible label; split navigation into three focused transitions. In the final full run these transition tests took 7.8–9.8 seconds each, explaining why combining them exceeded 15 seconds. The cancellation and delete-conflict tests passed in multiple full attempts; final times were 7.5 and 8.0 seconds. No timeout was increased or assertions disabled.
- Repeated focused stability run: **10 passed**, 88 unselected by name filter, 0 failed; 3 files in 36.93 seconds. This followed the green full suite and includes detached-trigger fallback, stale queued-action protection, all three module transitions and both Timesheet regressions. TypeScript, ESLint and Prettier passed again afterward.
 No browser screenshots or viewport-based visual checks have been performed. Full Git history secret scanning and production-hardening review remain outside the evidence obtained here.

## Windows acceptance

Ensure an active Node runtime is selected (`node --version`). This machine has Node 24.22.2 installed under the NVM installs directory, but its default launcher reports no active version. Start Docker Desktop in Linux-container mode and confirm `docker info` succeeds. Then execute the README verification commands. Check 375/768/1280/1440px layouts in both themes, keyboard action menus and dialog focus, structured Timesheet options and module navigation resets.

## Delivery state

Changes remain local and uncommitted on the authorized checkpoint branch. Original staged changes are unchanged (8 files, 68 insertions, 20 deletions); this work is additional unstaged/new source and documentation. No push or comparison URL was created. Diagnostic logs are retained under ignored `target/audit-logs/`; generated frontend build files remain ignored under `frontend/dist/`.

Remaining acceptance: start Docker Desktop's Linux engine, confirm `docker info`, then run `./mvnw.cmd clean verify` to obtain PostgreSQL integration and JaCoCo results. Browser inspection at representative widths and in both themes has not been performed; DOM/style regressions do not substitute for visual acceptance. Source-only common-secret-pattern review found no matches in modified files; full Git history and private-data review are not complete.


### Subsequent visual acceptance review

The later successful backend run (562 passed, coverage met) supersedes the earlier Docker blocker recorded above. A real-browser acceptance pass and narrowly scoped follow-up fixes are documented in [visual-acceptance-review.md](visual-acceptance-review.md), with genuine disposable-data screenshots and an explicit coverage/limitations matrix.
