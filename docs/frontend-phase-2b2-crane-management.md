# Phase 2B-2 — Crane management

## Scope and baseline

The real `/workspace/cranes` directory replaces the placeholder on the existing `feat/react-phase-2b2-crane-management` branch. The branch was checked against remote main and includes the developer's activation commit `f0cc46a544339255b80520008b76e2755470bf42`. That commit already supplied the Boolean response field, validated PATCH request, controller endpoint, mapping and PostgreSQL activation round-trip test; this phase preserves them.

The directory supports listing, create, edit, details, reversible activation, five-field case-insensitive search, combined activation/operational filters and client-side pagination. It reuses Customer Management conventions and the MUI design system. No job, maintenance, quotation, invoice or user frontend module is implemented. No dependencies, database schemas, numbering algorithms, JWT/session architecture or permission policies are changed.

## API contract and authorization

| Method | URL | Behavior |
| --- | --- | --- |
| GET | `/api/cranes` | Full array, including inactive records; no search/pagination parameters. |
| GET | `/api/cranes/{id}` | Current record for view/edit. |
| POST | `/api/cranes` | Creates an active crane with AVAILABLE operational status and backend-generated number. |
| PUT | `/api/cranes/{id}` | Updates six business fields; preserves ID, crane number, status and activation. |
| PATCH | `/api/cranes/{id}/activation` | Required Boolean `{"active":true}` or `{"active":false}`; returns updated CraneResponse. |
| DELETE | `/api/cranes/{id}` | Existing 204 soft-deactivation; now shares PATCH safety checks. |

Create/update body: `type: string`, `capacity: number`, `manufacturer: string`, `serialNumber: string`, `year: integer`, `location: string`. Required strings are trimmed; capacity must be positive and year a positive integer. No arbitrary year/type limits are added. React never submits ID, crane number, status or active in these bodies.

Response: `id: number`, `craneNumber: string`, `type: string`, `capacity: number`, `manufacturer: string | null`, `serialNumber: string`, `year: number`, `status: AVAILABLE | IN_USE | MAINTENANCE`, `active: boolean`, `location: string | null`. The existing nullable database fields display a fallback. No response schema is changed by this phase.

| Role | List/detail | Create/edit | PATCH/DELETE |
| --- | --- | --- | --- |
| ADMIN | Allowed | Allowed | Allowed |
| SALES | Allowed | Allowed | Allowed |
| OPERATIONS | Forbidden | Forbidden | Forbidden |
| FINANCE | Forbidden | Forbidden | Forbidden |
| MAINTENANCE | Forbidden | Forbidden | Forbidden |

Existing controller-level `@PreAuthorize` and enabled method security remain authoritative. Frontend navigation and Protected routes match them. Existing limited crane lookup access for other roles is unchanged; lookup access does not grant Crane Management access.

Missing/null activation returns 400; unknown IDs return 404; unauthenticated requests 401; forbidden roles 403; unfinished work or duplicate serial conflicts 409. Existing ProblemDetail normalization handles errors. Serial uniqueness already has a PostgreSQL unique constraint and global 409 translation; an integration regression verifies failed duplicate updates leave stored fields intact. No speculative serial service rewrite was needed.

## Activation safety policy

Source review found that job creation/reassignment and maintenance scheduling validate active cranes. Deactivating a crane with unfinished assignments could therefore make those records inconsistent with subsequent edits. Both deactivation entry points now call the same service rule:

- Reject deactivation when **any PENDING job** references the crane.
- Reject deactivation when **any SCHEDULED or IN_PROGRESS maintenance** references it.
- Check lifecycle state independently of dates, including future bookings and unfinished past work. Terminal history does not block deactivation.
- Return HTTP 409 explaining that unfinished work must first be finished or canceled using its existing workflow. Do not automatically transition that work.
- Acquire the existing `findByIdForUpdate` crane row lock inside a transaction, coordinating with the existing scheduling locks. Use repository existence queries rather than loading full related collections.
- Reactivation remains allowed and changes only `active`; repeated assignments are safe. Deactivation still validates unfinished work even if legacy data was already inactive.

`active` is register eligibility, not availability. AVAILABLE/IN_USE/MAINTENANCE remains read-only and controlled by existing job/maintenance workflows. Reactivation never forces AVAILABLE. No automatic status synchronization was added. Active-only lookup queries already filter the Boolean: inactive cranes disappear, reactivated cranes reappear, and separate scheduling/availability validation still applies.

Historical records remain linked; neither endpoint physically deletes a crane. The existing three-digit generated suffix (for example `CR080T001`) and all stored crane numbers are unchanged, including after type edits. Existing numbering regression tests remain in place.

### Inactive-crane maintenance restrictions

Maintenance creation and updates require the selected crane to be active.

- `POST /api/maintenance` rejects an inactive crane with HTTP 409 Conflict.
- `PUT /api/maintenance/{id}` rejects an inactive crane with HTTP 409 Conflict, including when the existing maintenance record references that inactive crane.
- Validation occurs within the existing transactional scheduling workflow.
- A rejected request must not modify the persisted maintenance record.
- Existing maintenance start, completion and cancellation workflows remain unchanged.
- Crane activation and operational status continue to represent separate business concepts.

**Verification:** Four additional regression tests were added: two service unit tests and two PostgreSQL integration tests. Full Maven verification passed with 552 tests, zero failures, zero errors, zero skipped tests and all JaCoCo coverage thresholds satisfied.

## Frontend architecture and UX

The five feature files mirror Customer Management without replacing its components. Typed API calls use the shared Axios client and AbortSignal for reads. `craneKeys.all = ['cranes']`; detail keys extend that prefix with the actual ID. Detail/edit fetch current records rather than trusting directory snapshots.

`PageHeader`, `DataTableShell`, `TableToolbar`, `FilterToolbar`, `SearchField`, `StatusChip`, `ActivationControl`, `ConfirmDialog` and shared states are reused. The only shell change removes Cranes' Planned badge. Selected navigation retains its background and focus outline without an inset stripe.

Search covers crane number, type, manufacturer, serial number and location. All/Active/Inactive and All/Available/In use/Maintenance combine with search. Search/filter changes reset the page; refreshed counts clamp it. Page sizes follow the shared table. Horizontal scrolling is contained within the table on narrow viewports.

The shared switch displays the server Boolean. Both activation directions require confirmation; Cancel sends no request. Pending controls/dismissal are disabled and a synchronous guard prevents duplicate submissions. No optimistic status change is stored. Failures retain the dialog and actual state, with retry available. Successful create/edit/activation invalidates crane list/detail and the existing `['lookups']` namespace, then shows feedback. Dialogs restore focus to the initiating control or the refresh button if a filtered row disappears.

Forms use React Hook Form/Zod, safe numeric conversion, accessible labels, inline validation and normalized server errors. Backend-controlled properties have no editable inputs. Detail views show all response fields, with separate status and activation. Light/dark themes and reduced-motion behavior remain intact.

## Changed files

Paths under `src/main/java/com/uvindu/crane_management_system/`:

| File | Change |
| --- | --- |
| `repository/JobCardRepository.java` | Existence query for pending crane assignments. |
| `service/CraneService.java` | Locked, shared PATCH/DELETE deactivation checks; preserve mapping/status/numbering. |

Frontend files:

| File | Change |
| --- | --- |
| `frontend/src/app/App.tsx` | Route the existing protected crane URL to CranesPage. |
| `frontend/src/app/AppShell.tsx` | Remove only the Cranes Planned badge. |
| `frontend/src/features/cranes/cranesApi.ts` | Actual response/request types, API methods, query keys. |
| `frontend/src/features/cranes/craneSchema.ts` | Six-field validation, labels and nullable edit defaults. |
| `frontend/src/features/cranes/CranesPage.tsx` | Directory, filters, pagination, activation and row actions. |
| `frontend/src/features/cranes/CraneFormDialog.tsx` | Create/edit, pending guards, field errors and invalidation. |
| `frontend/src/features/cranes/CraneDetailsDialog.tsx` | Fresh detail/edit loading, missing/error states and read-only fields. |

Tests and documentation:

| File | Change |
| --- | --- |
| `src/test/java/com/uvindu/crane_management_system/service/CraneServiceTest.java` | Locked loading, repeat activation, preserved fields and unsafe deactivation checks. |
| `src/test/java/com/uvindu/crane_management_system/controller/CraneControllerTest.java` | Activation delegation and response unit test. |
| `src/test/java/com/uvindu/crane_management_system/integration/CraneIntegrationTest.java` | PostgreSQL persistence/lookup behavior, all-role CRUD/activation, validation, blocked work, preserved history and duplicate update rollback. Existing round-trip test retained. |
| `frontend/src/test/cranes.test.tsx` | MSW-backed permissions, directory, CRUD, details, filters, pagination, validation, pending/cancel/retry/error, refreshed state and focus checks. |
| `docs/frontend-phase-2b2-crane-management.md` | This implementation/verification/acceptance record. |

## Verification

Final clean verification on 2026-10-27:

| Gate | Actual result |
| --- | --- |
| Maven clean verify | BUILD SUCCESS; 548 tests, 0 failures, 0 errors, 0 skipped. |
| JaCoCo | All thresholds passed, unchanged. 46/46 classes covered (100%); instructions 98.33%, branches 87.58%, lines 99.25%, methods 94.36%. Required minima: 90% instruction/line/method, 70% branch, 100% class. |
| Vitest | 129 passed across 5 files, no failures/skips; includes the existing customer, authentication and UI suites. |
| TypeScript | Passed. |
| ESLint | Passed. |
| Prettier format:check | Passed. |
| Vite production build | Passed. |
| npm audit | 0 vulnerabilities. |

The new coverage adds 14 backend cases to the 534-test baseline and 35 frontend cases to the 94-test baseline. Backend tests verify the real HTTP security/validation and PostgreSQL data; the direct controller unit test verifies delegation only. No test/coverage gate was disabled. The final verification commands had no failures.

Commands executed from repository root:

```bash
JAVA_HOME=/workspace/jdk21 MAVEN_USER_HOME=/workspace/.m2 ./mvnw -Dmaven.repo.local=/workspace/.m2/repository clean verify
```

A full JDK 21 and writable Maven cache were used. A disposable local PostgreSQL container and generated process-only JWT secret supplied the application-context test prerequisites; integration tests used the unchanged PostgreSQL Testcontainers configuration. No credentials were committed.

Commands executed from frontend (equivalently with `--prefix frontend`):

```bash
npm run format
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
npm audit
```

Dependencies already installed from the existing lockfile were reused; no package or lockfile changed. Zod pure-annotation build warnings remain non-fatal and unsuppressed.

An external temporary Playwright harness with system Chromium exercised the real frontend at 390, 768, 1366 and 1440 pixels in both themes (eight runs): login/navigation, list/search/pagination, details, edit, both activation directions, creation and document overflow. All eight passed with zero page errors. Desktop light and mobile dark screenshots were visually inspected. The harness intercepted API fixtures; this is browser smoke verification, not live-backend or developer manual acceptance. PostgreSQL behavior is separately covered by the integration suite. No browser tooling was added to the application.

## Manual acceptance — IntelliJ IDEA, VS Code, Chrome and PostgreSQL

Use a disposable development database and test accounts, never production credentials/data. These steps remain for developer acceptance.

1. **IntelliJ IDEA:** check out `feat/react-phase-2b2-crane-management`, open the root Maven project, choose a full JDK 21 for Project SDK and Maven Runner, and reload Maven. Start Docker. Run `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) with non-production `DB_PASSWORD` and a strong `JWT_SECRET` in environment variables. The application-context test uses local PostgreSQL; integration tests start Testcontainers.
2. Configure `CraneManagementSystemApplication` with the same variables and PostgreSQL `crane_management_system` on localhost:5432, username postgres, or override `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` for your isolated database. Start the application on port 8080. In IntelliJ Database tools connect to that database, keeping secrets out of committed configuration. Obtain existing test accounts for ADMIN, SALES, OPERATIONS, FINANCE and MAINTENANCE through the established admin workflow.
3. **VS Code:** open `frontend`, run `npm ci`, then each frontend command above (format is optional for acceptance; format:check is required). Run `npm run dev`. The unchanged Vite proxy sends `/api` to localhost:8080.
4. **Chrome/listing:** open localhost:5173, sign in as ADMIN, open Cranes and DevTools Network (Fetch/XHR). GET `/api/cranes` should return real PostgreSQL rows including inactive ones. Compare number, fields, operational chip and activation switch with the database. Do not paste tokens into reports or shared screenshots.
5. **Create:** Add crane with a unique test serial, valid nonempty type/manufacturer/location, positive capacity and positive integer year. Verify POST contains exactly the six editable fields, returns 201, and backend-generated number, AVAILABLE status and active=true appear after refresh. Inspect the persisted row. For a fresh type `080T`, the existing generator should produce `CR080T001`; existing records determine the next suffix. Test empty/negative/fractional-year validation and duplicate serial rejection.
6. **Details/edit:** use the row menu. Verify details show every response field. Edit location/capacity and save: PUT uses the actual ID and six fields; list/detail refresh and PostgreSQL agree. Number, ID, active and operational status must not change. A duplicate serial should return 409 with entered values retained and no stored field changes.
7. **Cancel deactivation:** on an unassigned test crane click the switch and Cancel. Network must show no PATCH; the switch and DB stay active. Repeat with keyboard Space/Tab/Escape and check focus return.
8. **Confirm/persist/reactivate:** confirm Deactivate crane. Verify PATCH `/api/cranes/{id}/activation` sends `{"active":false}`, receives 200 with false and unchanged number/status, then refreshes GET. Reload Chrome (sign in again because sessions are memory-only) and verify Inactive persists. Confirm Activate crane and verify `{"active":true}` and persisted true. Repeat identical PATCH bodies using Chrome Edit and Resend on test requests; expect safe 200 responses when no unfinished work blocks deactivation.
9. **PostgreSQL:** for the actual test ID, inspect `SELECT id, crane_number, serial_number, status, active FROM cranes WHERE id = <test-id>;` in IntelliJ's database console after each step. Check GET `/api/lookups/cranes` with an authorized test session: inactive ID absent, reactivated ID present. Existing historical job/maintenance references must remain unchanged. Legacy DELETE still returns 204 and only sets active=false when safe.
10. **Search/filter/pagination:** have more than five test cranes, including inactive and different operational states created through the established workflows. Search each supported string case-insensitively, combine activation and operational chips, clear filters and switch page sizes/pages. Verify filter changes reset the page. Deactivate the last Active-filtered row on the final page; the page must clamp and focus remain usable.
11. **RBAC:** repeat read/create/edit/activation as SALES. OPERATIONS, FINANCE and MAINTENANCE must have no Cranes navigation and direct `/workspace/cranes` must show Access denied without fetching cranes. With each test session's authenticated API request, all crane-management endpoints must return 403 for those three roles. Unauthenticated requests return 401; ADMIN null/missing activation returns 400; unknown ID 404. Limited lookup permissions remain distinct.
12. **Operational safety:** use existing backend job/maintenance workflows to assign a PENDING job, SCHEDULED maintenance and IN_PROGRESS maintenance to separate test cranes. Both PATCH false and DELETE must return 409, leaving DB active=true and relationships intact. Complete/cancel through existing supported endpoints, then retry deactivation successfully. Check future and unfinished past work too. Reactivation must never force AVAILABLE or alter existing operational history.
13. **Errors/pending:** throttle Network, submit activation/create/edit and check disabled controls, no duplicate requests and blocked confirmation dismissal. Block the mutation request in DevTools, verify visible error and unchanged activation/retained form values, unblock and retry. Simulate missing detail/temporary list error in a test environment, verify missing state or Retry. Existing expired-token behavior must return to sign-in.
14. **Responsive/accessibility:** at 390, 768, 1366 and 1440 widths check both themes; table scroll stays inside its card; identifiers can be inspected in details; sidebar selected styling has no inset stripe. Check keyboard navigation, labeled switches, filter pressed states, form labels/errors, focus restoration, dialog scrolling and reduced-motion preference. A screen-reader check remains manual.
15. **Regression:** verify Customers listing/search/filter/pagination, create/edit/details and reversible activation; dashboard/API data, logout and role navigation remain working. Do not merge the draft PR until review and developer acceptance.

## Remaining limitations

The directory uses the existing full-array API and client-side filtering/pagination; very large fleets may eventually need a separately designed server pagination contract. Current form editing retains the application's existing concurrency behavior; no version field/schema or conflict-resolution system is added. The new row lock coordinates activation with current job/maintenance assignment code, not arbitrary external SQL writers.

Safety relies on persisted lifecycle states, not a speculative inference from crane.status or dates. Legacy inactive cranes with unfinished work can be reactivated, but false requests remain blocked until work is finished/canceled. Existing operational-status synchronization and scheduling policies are unchanged; an active crane can still be unavailable for a specific job. Historical links remain stored and were checked for canceled jobs and maintenance. No migration/repair of historical inconsistencies is performed.

Full live-browser acceptance, screen-reader evaluation and business approval of the documented deactivation policy remain with the reviewer. No PR merge, ready-for-review transition or work on subsequent modules is authorized by this delivery.

## Frontend test stabilization — 2026-10-29

This follow-up starts from remote branch commit `25943c6` and preserves its manually gated Customer activation test, all maintenance corrections and the theme-aware inactive-row styling. No production source, dependency, lockfile, npm script, Vitest worker setting or timeout is changed. PR #7 remains draft.

### Diagnosis and corrections

The developer reported eight 5-second timeouts on Windows 11. The unmodified latest branch passed 129/129 in this Linux environment (approximately 34.7 seconds from Vitest's JSON timestamps). Those Windows failures were not reproduced here, so host-specific CPU/worker contention is a contributing hypothesis, not a proven configuration defect. The actual test weaknesses were:

- Each customer/crane scenario typed login and business fields character by character, repeatedly exercising MUI and form updates even when the assertion concerned only final values. Several customer tests took 2–2.5 seconds locally before the change. Final-value field setup/search now uses `fireEvent.change`, including safe numeric input conversion through the actual registered inputs. Real sign-in, `/auth/login`, `/auth/me`, Protected routes and permission checks still run. Auth-specific typing, user clicks/double clicks, Escape and focus checks remain.
- Fixed 150–400 ms MSW delays made loading/pending checks depend on execution speed: the response could complete before an assertion, or Escape could be sent after pending had ended. The small `createRequestGate` helper announces request start and holds the response until explicitly released in `finally`. Both activation directions and creation test duplicate submission, disabled actions and Escape before release, then verify completion/refetch. Customer activation retains the developer's Promise-based correction with this shared cleanup pattern.
- Several failure tests waited only for an enabled submit button. That button could still be enabled before the request started, producing a false pass. They now wait for the actual normalized error, including the 403 Access denied state, then verify retained inputs/activation and no success message.
- Dialog cancellation explicitly waits for removal before opening the next dialog. Existing transitions, focus restoration, query invalidation, MSW unhandled-request errors, test isolation and cleanup are preserved. No evidence justified changing those implementations or bypassing authentication.

Production pending guards behaved correctly. No production defect was identified or altered in this task. Inactive rows remain muted, readable and interactive. No tests were removed, skipped or added; the suite remains 129 cases in five files. No warnings were suppressed.

Changed files:

| File | Purpose |
| --- | --- |
| `frontend/src/test/cranes.test.tsx` | Efficient field setup; deterministic loading/create/activation; retained payload, role, filtering, refetch and focus coverage. |
| `frontend/src/test/customers.test.tsx` | Same improvements; preserve/strengthen existing gated activation correction and await real errors. |
| `frontend/src/test/app.test.tsx` | Gate identity/dashboard loading responses instead of relying on elapsed milliseconds; retain realistic login typing. |
| `frontend/src/test/requestGate.ts` | Small shared MSW response gate with request-start notification and explicit release. |
| `docs/frontend-phase-2b2-crane-management.md` | This diagnostic and verification record. |

### Executed verification

Every run below used the unchanged default 5-second test timeout. Whole-suite runs were sequential, not overlapping.

| Run | Passed | Failed | Skipped | Duration |
| --- | ---: | ---: | ---: | --- |
| Unmodified branch, normal workers, JSON reporter | 129 | 0 | 0 | ~34.7 s |
| Corrected cranes, `--maxWorkers=1` | 35 | 0 | 0 | 19.54 s |
| Corrected customers, `--maxWorkers=1` | 46 | 0 | 0 | 30.27 s |
| Complete suite, `--maxWorkers=1` | 129 | 0 | 0 | 57.28 s |
| Standard `npm run test`, repeat 1 | 129 | 0 | 0 | 29.96 s |
| Standard `npm run test`, repeat 2 | 129 | 0 | 0 | 35.07 s |
| Standard `npm run test`, repeat 3 | 129 | 0 | 0 | 29.54 s |

An earlier development run of the revised crane tests had 33 passes and two failures: newly added clicks on disabled MUI buttons were correctly rejected by user-event's pointer checks. Those invalid interactions were removed; explicit disabled assertions, real double clicks on the enabled submit button, single-request counts and pending Escape checks remain. The corrected targeted run and full runs above passed. One initial command was invoked from the repository root rather than `frontend`; it exited before running tests and was rerun in the correct directory.

`npm run typecheck`, `npm run lint`, `npm run format:check` and `npm run build` all passed. Only the four edited test/helper files were formatted. Existing non-fatal Zod annotation build warnings remain visible. No Java changes were made and the backend suite was not rerun for this frontend-only follow-up; earlier backend verification above is historical.

Normal full-suite runtime was approximately 34.7 seconds before, and 29.96 / 35.07 / 29.54 seconds after (31.52 seconds average). This modest improvement varies with host load; do not interpret it as a controlled Windows benchmark. In the corrected single-worker run the slowest test was 1.77 seconds, and the previously reported crane create/edit cases took 0.99 / 0.89 seconds. The key reliability improvement is explicit response ordering, rather than relying on a faster machine to win timing races.

Windows 11/PowerShell verification remains a platform limitation of this Linux runner. Run the unchanged `npm run test` in `frontend` after pulling this branch; do not compensate by raising timeouts. Three repeat runs provide useful local stability evidence, not a guarantee under every machine load. Live-backend manual acceptance already reported by the developer is preserved; this test-only task does not claim a new browser acceptance run.

## Final synchronization and configuration correction — 2026-10-29

This section supersedes the earlier test-configuration notes for the latest branch. The remote already introduced a 15,000 ms timeout and single-worker npm command; this task preserves those settings and does not raise the timeout again.

### Confirmed findings

- The cloud checkout started clean at `37db9ae9211934fcb9909e291f1fa2c88097dab4`. Remote feature HEAD was `b659031fba29153e1711b453db8c684353e84bd7`, six commits ahead. The old checkout contained both the 5-second defaults and the exact Confirm-focus assertion reported in the developer's logs. A safe fast-forward brought in all six existing fixes; no local work or history was discarded.
- This clone's `remote.origin.fetch` mapped only main. The feature branch's configured upstream could not be resolved and a plain fetch would not refresh its tracking ref. The existing main mapping was retained and the feature mapping added with `git remote set-branches --add origin feat/react-phase-2b2-crane-management`; the existing feature branch now tracks its remote correctly. This is local Git configuration, not a committed repository file. The diagnostic command exposes the mappings so another clone can be checked independently.
- Resolving Vitest configuration from the actual frontend directory showed that npm test used 15,000 ms and one worker. Config-only/direct Vitest resolved 15,000 ms but no worker cap. `maxWorkers: 1` now lives in the shared Vite test config as well, so normal direct/IDE runs get the same defaults. Explicit IDE/CLI overrides can still differ.
- The remote focus-containment correction introduced two TypeScript errors: `document.activeElement` is `Element | null`, while jest-dom's `toContainElement` typing accepts a narrower element type. Native DOM `dialog.contains(document.activeElement)` preserves the same containment assertion without a cast; `waitFor` allows the real MUI focus trap to settle after both Tab directions. Safe Cancel focus, Escape dismissal and focus restoration remain checked.
- Prettier reported `frontend/eslint.config.js`; only affected files were formatted. Existing generated-cache exclusions are preserved. There was no evidence here that formatting reverted commits or that MSW gates/application code needed further changes.

The developer's Windows working tree is not accessible from this Linux runner. The stale cloud checkout is confirmed; the precise source of the Windows logs (older clone, unsaved file, old process, alternate config or explicit timeout override) cannot be confirmed remotely. A log showing 5,000 ms or the old Confirm assertion does not match the verified standard command at the latest feature HEAD.

### Files changed in this follow-up

| File | Change |
| --- | --- |
| `frontend/vite.config.ts` | Match direct Vitest/IDE worker defaults to npm test; retain 15,000 ms. |
| `frontend/src/test/refinement.test.tsx` | Type-correct, asynchronous focus containment checks; preserve all keyboard assertions. |
| `frontend/scripts/test-environment.mjs` | Read-only diagnostics using Vitest's actual config resolver: root/config paths, Git branch/SHA/upstream/refspecs/status, Node/platform, npm command and effective defaults. No automatic Git mutations. |
| `frontend/package.json` | Add `npm run test:doctor`; existing test command unchanged. |
| `frontend/eslint.config.js` | Fix formatting and apply existing Node globals to the diagnostic script; preserve lint rules/exclusions. |
| `docs/frontend-phase-2b2-crane-management.md` | Findings, execution results and safe Windows synchronization instructions. |

No backend, application components, MSW gates, inactive-row styling, dependencies or lockfiles changed. All 129 tests remain enabled.

### Verification on Linux

The fetched baseline passed 129/129 tests in 59.69 seconds, but typecheck exposed the two focus assertion errors and format:check exposed the ESLint-config formatting problem. These failures were corrected rather than bypassed.

| Final full-suite run | Passed | Failed | Skipped | Duration |
| --- | ---: | ---: | ---: | --- |
| 1 | 129 | 0 | 0 | 61.84 s |
| 2 | 129 | 0 | 0 | 59.07 s |
| 3 | 129 | 0 | 0 | 60.09 s |

All three runs were consecutive with the standard npm command and the remote's existing 15-second timeout. `npm run typecheck`, `npm run lint`, `npm run format:check`, and `npm run build` passed after the corrections. `npm run test:doctor` also ran successfully. Node v24.19.0 / npm 11.9.0 were used. No tests were added, removed or skipped.

`npm run test:doctor` resolves both npm and direct Vitest to the actual `frontend/vite.config.ts`, jsdom, `src/test/setup.ts`, one worker and 15,000 ms. Test runs use the existing `npm run test` command without additional overrides. No new browser/Windows acceptance or backend test run is claimed. Existing non-fatal Zod annotation build warnings remain visible.

### Exact Windows 11 / PowerShell verification

1. Stop old test/watch tasks in the VS Code terminals and Testing panel. Save intentional edits. In the intended repository clone, inspect and synchronize safely:

```powershell
Set-Location 'C:\path\to\Crane-Management-System'
$branch = 'feat/react-phase-2b2-crane-management'
git rev-parse --show-toplevel
git branch --show-current
git status --short
if (git status --porcelain) {
    throw 'Working tree has local work. Review and preserve it before synchronizing; do not reset or clean it.'
}
git fetch origin "refs/heads/${branch}:refs/remotes/origin/${branch}"
if ($LASTEXITCODE -ne 0) { throw 'Fetch failed; stop and inspect.' }
git switch $branch
if ($LASTEXITCODE -ne 0) { throw 'Branch switch failed; stop and inspect.' }
git merge --ff-only "origin/$branch"
if ($LASTEXITCODE -ne 0) { throw 'History diverged; preserve commits and review before continuing.' }
git rev-parse HEAD
git rev-parse "origin/$branch"
git rev-list --left-right --count "HEAD...origin/$branch"
git config --get-all remote.origin.fetch
```

Expected: the intended branch, HEAD matching the latest remote feature SHA, and `0 0` divergence. The explicit fetch above works even for a main-only fetch mapping. If the mappings omit both `refs/heads/*` and this feature branch, add only the missing mapping, retaining other mappings:

```powershell
git remote set-branches --add origin feat/react-phase-2b2-crane-management
git fetch origin
git branch --set-upstream-to=origin/feat/react-phase-2b2-crane-management
```

2. Anchor subsequent commands to this clone's frontend, not another VS Code workspace folder:

```powershell
$repo = git rev-parse --show-toplevel
Set-Location (Join-Path $repo 'frontend')
node --version
npm --version
npm ci
if ($LASTEXITCODE -ne 0) { throw 'Dependency install failed.' }
npm run test:doctor
if ($LASTEXITCODE -ne 0) { throw 'Test configuration could not be resolved.' }
```

Expected diagnostic output: branch `feat/react-phase-2b2-crane-management`, your newly synchronized SHA, frontend root/config/setup paths in this clone, command `vitest run --maxWorkers=1 --testTimeout=15000`, and `maxWorkers: 1` / `testTimeout: 15000` for **both** npmTest and directVitest. Git upstream metadata reflects the last fetch. Do not delete the lockfile or discard local changes to troubleshoot this.

3. Run the requested checks with failure detection:

```powershell
foreach ($run in 1..3) {
    Write-Host "Full suite run $run"
    npm run test
    if ($LASTEXITCODE -ne 0) { throw "Test run $run failed." }
}
foreach ($check in 'typecheck', 'lint', 'format:check', 'build') {
    npm run $check
    if ($LASTEXITCODE -ne 0) { throw "$check failed." }
}
```

Each test run must report 129 passed, no failures/skips. If terminal output still says 5,000 ms, inspect the printed npm script and diagnostic paths/SHA first. An explicit editor `--testTimeout=5000`, alternate `--config`, per-test override, old watch process or different workspace can override/bypass the standard invocation. Restart the VS Code test runner against this frontend's `vite.config.ts` after updating; compare its logged command to the terminal run. Do not increase the timeout again to conceal a mismatch.
