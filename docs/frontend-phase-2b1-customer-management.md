# Phase 2B-1 — Customer management and reversible activation

## Current implementation

The API-backed customer directory retains create/edit/detail, nullable field handling, search, email filtering and client-side pagination. The activation revision replaces the deactivate-only interface with a reusable, controlled MUI switch. ADMIN and SALES can change it; OPERATIONS sees a disabled switch with the actual state; FINANCE and MAINTENANCE retain denied customer access.

Switch changes require confirmation (including reactivation). Deactivation explains that inactive customers disappear from active-only lookups while existing records and relationships remain. Pending requests disable activation controls and confirmation dismissal. Failures are displayed inside the dialog without changing displayed state. Success invalidates `['customers']` (including details) and the `['lookups']` query namespace. No optimistic activation state is stored. All/Active/Inactive filters use the server Boolean; filters reset pagination and counts remain bounded after refresh.

`ActivationControl` takes only a real Boolean, accessible entity name, disabled state and change callback. It owns no fetching or operational-status logic. No crane, quotation, job, user or invoice module was changed.

The sidebar's selected inset accent shadow was removed for all items; selected backgrounds, radius, hover and visible focus outlines are preserved.

## API and persistence

| Method | URL | Behavior |
| --- | --- | --- |
| GET | `/api/customers` | All customers, including inactive rows. |
| GET | `/api/customers/{id}` | Detail with actual `active`. |
| POST | `/api/customers` | Create; existing entity default is active. |
| PUT | `/api/customers/{id}` | Update business fields; activation is unchanged. |
| PATCH | `/api/customers/{id}/activation` | Required Boolean body `{"active":true}` or `{"active":false}`; returns updated CustomerResponse. ADMIN/SALES only. |
| DELETE | `/api/customers/{id}` | Existing 204 soft-deactivation remains compatible. |

CustomerResponse adds `active` to `id`, companyName, registrationNo, vatNo, companyAddress, billAddress, contactPerson, contactNo and customerEmail. Optional response fields retain null handling. Create/edit requests still contain only the eight business fields, never id or active. Required text is trimmed; phone is a string matching `^0\d{9}$`; email is optional. Existing ProblemDetail handling and JWT behavior are unchanged.

Activation uses a transaction and the existing entity Boolean; no schema/data migration. Missing customers return 404; missing/null activation values return 400; repeated identical assignments are valid. Existing active-only lookup queries already filter `active = true` and need no repository change. Reactivation restores lookup eligibility. Related quotations/job cards are not deleted, rewritten or transitioned. Their existing service validations remain authoritative.

The full array is searched/paginated locally. No unsupported server query parameters or operational status toggles were introduced. Deploy the backend response extension before the updated frontend. Concurrent writers follow the application's existing last-write behavior; this phase adds no version/conflict schema.

## Files changed in this revision

| File | Reason |
| --- | --- |
| `src/main/java/com/uvindu/crane_management_system/dto/CustomerActivationRequest.java` | Validated, non-null Boolean activation request. |
| `src/main/java/com/uvindu/crane_management_system/dto/CustomerResponse.java` | Expose persisted activation flag. |
| `src/main/java/com/uvindu/crane_management_system/controller/CustomerController.java` | Explicit role-protected PATCH endpoint. |
| `src/main/java/com/uvindu/crane_management_system/service/CustomerService.java` | Transactional assignment and response mapping. |
| `src/test/java/com/uvindu/crane_management_system/service/CustomerServiceTest.java` | Both directions, repetition, preserved fields and missing ID. |
| `src/test/java/com/uvindu/crane_management_system/integration/CustomerIntegrationTest.java` | PostgreSQL persistence, serialization, lookup eligibility, all five roles, null/missing input and missing customer. |
| `frontend/src/shared/ui/ActivationControl.tsx` | Reusable accessible controlled activation switch. |
| `frontend/src/features/customers/customersApi.ts` | Boolean response type and typed PATCH call. |
| `frontend/src/features/customers/CustomersPage.tsx` | Server-backed switch, filters, confirmation, pending/error feedback and invalidation. |
| `frontend/src/app/AppShell.tsx` | Remove selected inset line only. |
| `frontend/src/test/customers.test.tsx` | Switch permissions, confirmation/cancel/pending/failure/refresh/reactivation and filters; retain CRUD tests. |
| `frontend/src/test/ui-polish.test.tsx` | Regression for selected styling without inset shadow. |
| `docs/frontend-phase-2b1-customer-management.md` | Updated contract, verification and manual steps. |

## Verification

**Backend:** 533 tests passed, 0 failures, 0 errors, 0 skipped. Full Maven clean verify succeeded; all JaCoCo thresholds passed (instruction/line 90%, branch 70%). **Frontend:** 94 tests passed, 0 failed, 0 skipped; TypeScript, lint, formatting and production build passed. npm audit reported 0 vulnerabilities.

Commands: `./mvnw clean verify`; frontend `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run test`, `npm run build`, `npm audit`. Formatting was applied before final checks. In this cloud environment Maven used `JAVA_HOME=/workspace/jdk21`, `MAVEN_USER_HOME=/workspace/.m2` and `-Dmaven.repo.local=/workspace/.m2/repository`. Temporary generated credentials/JWT secret stayed in process environment; no secrets were committed.

Initial attempts failed because the default Maven cache was read-only, the system Java installation lacked the JDK 21 compiler, and tests lacked JWT_SECRET/local PostgreSQL for the application-context test. A full temporary JDK and an isolated disposable PostgreSQL instance resolved those prerequisites; integration tests retain the existing Testcontainers configuration. Initial frontend failures were test-role/handler/transition expectations from the deactivate-only flow; these were corrected to exercise actual switches and PATCH requests.

Existing ESLint/whatwg-encoding deprecation and Zod annotation warnings are not suppressed. Previous Phase 2B-1 screenshots under `docs/images/phase-2b1/` are historical, before activation controls. This revision does not claim fresh browser, screen-reader or live application acceptance testing.

## Exact manual verification — IntelliJ IDEA, VS Code and Chrome

1. **IntelliJ IDEA:** check out `feat/react-phase-2b1-customer-management`, open the root Maven project, set Project SDK and Maven Runner JRE to a full JDK 21, and reload Maven. Start Docker for Testcontainers. Use a non-production PostgreSQL database `crane_management_system` on localhost:5432 with the configured postgres user. Supply DB_PASSWORD and a strong JWT_SECRET through Run Configuration environment variables; never add them to source control. Run Maven `clean verify`. Run `CraneManagementSystemApplication` with the same environment; confirm port 8080.
2. **VS Code:** open `frontend`, run `npm ci`, then `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run test`, `npm run build`, and `npm audit`. Start `npm run dev`; the existing Vite proxy forwards `/api` to localhost:8080.
3. **Chrome:** open localhost:5173 and DevTools Network. Sign in as ADMIN with an existing test account; open Customers. Create a uniquely named test customer. Confirm GET/POST responses contain `active:true`, the switch is Active, and detail/edit still work.
4. Click the switch. Cancel once: no PATCH should be sent and state must remain Active. Repeat and confirm: expect PATCH `/api/customers/{id}/activation`, body `{"active":false}`, HTTP 200 with `active:false`, refreshed list and Inactive switch. Inspect the PostgreSQL row in IntelliJ Database tools to confirm persistence. Reload/sign in again and verify it stays inactive.
5. Filter Active and Inactive; test search, contact-email filter and page reset. Switch the inactive record back on, confirm, and verify PATCH `{"active":true}`, refreshed Active state and persisted Boolean. Repeating the same PATCH should return 200. Check the existing authorized active-only customer lookup: exclude when inactive, include when active. Existing quotations/job cards must remain intact.
6. Repeat as SALES. OPERATIONS must see actual status and disabled switches, without create/edit controls. FINANCE and MAINTENANCE must lack Customers navigation and see Access denied on direct `/workspace/customers`. With test JWTs, PATCH as each forbidden role must return 403; missing/null active as ADMIN must return 400; unknown ID must return 404.
7. Enable Chrome network throttling, confirm a change and verify switches/confirmation actions remain disabled until completion. Use DevTools request blocking to simulate a failed PATCH: no false status change, error inside confirmation, retry possible. Verify expired-token return to login. DELETE should still return 204 and soft-deactivate.
8. At widths 390, 768, 1366 and 1440, check both themes, keyboard Tab/Space activation, Cancel/Escape focus return, horizontal table scrolling, create/edit/details and sidebar selected background. The sidebar must have no left accent stripe while keyboard focus remains visible.

No auto-merge or Phase 2B-2 work is included.
