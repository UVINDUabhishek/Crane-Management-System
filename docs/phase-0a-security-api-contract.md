# Phase 0A: security and frontend API contracts

## Verified findings

Inspection of the original controllers, DTOs, services, filter, and tests confirmed:

1. `AdminController.createUser` returned the `User` entity, including its encoded password.
2. `CustomUserDetailsService` exposed the active flag as `UserDetails.isEnabled()`, but the JWT filter did not check it before establishing authentication. Password login already rejected disabled users.
3. No authenticated current-user endpoint existed. JWTs contain email and expiry but no role.
4. Quotation, timesheet, and user responses omitted their database IDs.
5. Job, quotation, timesheet, maintenance, and invoice responses contained relationship labels without the corresponding IDs.
6. Existing role checks prevented employees from obtaining some dropdown options, even when they could perform the associated operation. Existing integration fixtures sometimes obtained IDs directly from repositories, masking browser contract gaps.

## Changes

### Authentication and safe user responses

- `POST /api/admin/users` still requires ADMIN and returns HTTP 200, now with `UserResponse` rather than an entity. Its exact fields are `id`, `username`, `email`, and `role`; passwords and password hashes are absent. Entity-only fields previously serialized by this endpoint are intentionally removed.
- `GET /api/auth/me` returns HTTP 200 with the same four-field response for any active authenticated role. Identity is resolved from the authenticated principal, never a request-supplied user identifier.
- `/api/auth/me` returns 401 for missing/invalid authentication. Existing login URL, request, raw JWT response, one-hour expiry, BCrypt encoding, and existing authorization rules are preserved.
- The JWT filter checks the current database-backed enabled flag before establishing authentication. Tokens issued before deactivation now receive 401 on subsequent requests. Password login for inactive users remains denied. Existing generic authentication error text is preserved.
- No refresh, token blacklist, logout, or password-change revocation mechanism is introduced.

### Additive response schemas

All existing DTO fields and their meanings remain unchanged. New fields are JSON numbers backed by Java `Long`:

| Response | Added fields |
| --- | --- |
| `UserResponse` | `id` |
| `QuotationResponse` | `id`, `customerId` |
| `TimesheetResponse` | `id`, `jobCardId`, `customerId` |
| `JobCardResponse` | `customerId`, `quotationId`, `craneId`, `salesPersonId`, `operatorId` |
| `MaintenanceResponse` | `craneId`, `maintenancePersonId` |
| `InvoiceResponse` | `customerId`, `craneId`, `quotationId`, `jobCardId` |

Optional `operatorId` and `maintenancePersonId` are null when unassigned. Shared service mappers supply the new fields on list, detail, create, update, and transition responses. Existing request DTOs, URLs, status codes, and database schemas are unchanged. In particular, `InvoiceResponse.crane` continues to mean serial number; clients should use the new `craneId` for identity.

### Dedicated read-only lookups

All routes require authentication and return HTTP 200 with a plain array. Empty results are `[]`. They grant no write access or access to full administrative/business records.

| GET endpoint | Roles | Result fields |
| --- | --- | --- |
| `/api/lookups/cranes` | ADMIN, SALES, OPERATIONS, FINANCE, MAINTENANCE | `id`, `label` (crane number) |
| `/api/lookups/customers` | ADMIN, SALES, OPERATIONS, FINANCE | `id`, `label` (company name) |
| `/api/lookups/quotations` | ADMIN, SALES, OPERATIONS | `id`, `label` (quotation number), `customerId` |
| `/api/lookups/operators` | ADMIN, SALES, OPERATIONS | `id`, `label` (operator name) |
| `/api/lookups/sales-people` | ADMIN, SALES, OPERATIONS, FINANCE | `id`, `label` (username) |
| `/api/lookups/maintenance-people` | ADMIN, OPERATIONS, MAINTENANCE | `id`, `label` (username) |

- Crane/customer/quotation lookups include active records only. They do not imply availability, quotation validity, or compatibility; existing business services remain authoritative.
- Operators have no active flag; all operators are included.
- Sales-person options include active ADMIN and SALES users, because both roles can create jobs and become their recorded salesperson.
- Maintenance-person options include all active users. The existing maintenance service permits assignment of any user; this phase does not introduce a MAINTENANCE-only assignment rule.
- Lookup responses omit emails, password hashes, pricing, and full entity details. Quotation customer IDs allow a form to filter options without granting access to quotation pricing.
- Existing `/api/admin/**`, full crane/customer/quotation lists, and other business permissions are not broadened. Forbidden roles receive 403; anonymous requests receive 401.
- Use existing permitted `/api/job-card` reads for timesheet job selection. No new job lookup is needed.
- Lookup arrays are not paginated and have no ordering guarantee. Historical inactive-record selection and changed-role salesperson history remain future requirements, if needed; existing historical report endpoints are unchanged.

## Files changed

Paths below are relative to `src/main/java/com/uvindu/crane_management_system/` unless specified:

- `controller/AdminController.java`: safe creation response.
- `controller/AuthController.java`: authenticated identity endpoint.
- `controller/LookupController.java` (new): explicit per-lookup role authorization.
- `security/JwtAuthenticationFilter.java`: enabled-account check.
- `service/UserService.java`: safe registration/current-user mapping and ID.
- `service/LookupService.java` (new): minimal read-only option projections.
- `service/QuotationService.java`, `TimesheetService.java`, `JobCardService.java`, `MaintenanceService.java`, `InvoiceService.java`: additive ID mapping only.
- `dto/UserResponse.java`, `QuotationResponse.java`, `TimesheetResponse.java`, `JobCardResponse.java`, `MaintenanceResponse.java`, `InvoiceResponse.java`: additive IDs.
- `dto/LookupResponse.java`, `QuotationLookupResponse.java` (new): minimal lookup records.
- `mvnw`: executable bit enabled so the requested Unix command runs directly; script contents unchanged.
- `docs/phase-0a-security-api-contract.md`: this contract and verification record.

Tests under `src/test/java/com/uvindu/crane_management_system/`:

- Updated `controller/AdminControllerTest.java`, `controller/AuthControllerTest.java`, `service/UserServiceTest.java`, `security/JwtAuthenticationFilterTest.java`.
- Updated `integration/AuthIntegrationTest.java` to use the existing cascading Testcontainers reset, fixing a test-order-dependent foreign-key cleanup failure.
- Added `integration/IdentitySecurityIntegrationTest.java`, `LookupIntegrationTest.java`, `FrontendContractIntegrationTest.java`.

## Regression coverage

- Safe four-field user creation/profile responses, stable IDs, stored BCrypt hash, and absence of plaintext/hash in returned JSON.
- Current-user access for every role; anonymous/invalid tokens rejected; caller cannot select another identity by query parameter; current database role is reflected.
- Real login token succeeds before deactivation, then fails on identity, business, and lookup routes; password login also fails afterward.
- Full five-role/six-lookup access matrix, anonymous denial, minimal field sets, inactive-record filtering, and documented assignment eligibility.
- Lookup access does not unlock full admin/business APIs or mutation endpoints.
- API-returned IDs drive quotation-to-job-to-timesheet create/detail/update/delete requests, without inferring IDs from document numbers.
- Related IDs and retained labels for job, invoice, and maintenance responses, including nullable assignment cases.
- Unit coverage for disabled-token rejection, safe user registration, current-user lookup, and controller delegation.

## Validation

Full Maven verification uses the project's existing Testcontainers PostgreSQL configuration, Surefire suite, and unchanged JaCoCo thresholds. The final full run passed: **436 tests, 0 failures, 0 errors, 0 skipped**, including the existing 420 tests and 16 added tests. All JaCoCo checks passed: instruction 97.32%, branch 82.03%, line 98.92%, method 95.39%, and class 100%. Maven exited 0 with `BUILD SUCCESS`.

The first run exposed an older authentication-test cleanup dependency and two new test fixture/assertion issues; these were corrected before rerunning the complete `clean verify` suite. No assertions, tests, or coverage thresholds were disabled. `git diff --check` also passed.

Cloud environment setup (outside the repository): activate the prepared Java 21 JDK, set a writable Maven cache and proxy-aware Maven settings, and supply a runtime-only JWT secret. The application context smoke test also needs the local development PostgreSQL service. No JWT or database secret is added by this change.

Equivalent full command used here:

```sh
./mvnw -s /workspace/.m2/settings.xml -Dmaven.repo.local=/workspace/.m2/repository -B -ntp clean verify
```

On a normally configured development machine, use `./mvnw clean verify` (Windows: `mvnw.cmd clean verify`).

## Remaining scope and risks

- No changes to job, invoice, maintenance, scheduling, pricing, reporting calculations, or their validation rules. Previously reported defects in those areas remain for a separately approved phase.
- No schema changes, new dependencies, React changes, CORS policy changes, or general exception-format redesign.
- Deactivation takes effect on the next request after its database commit; already executing requests are not canceled.
- Password changes do not revoke tokens; disabled accounts are checked through the existing per-request user load.
- Additive DTO fields are compatible with normal clients, but consumers using closed/strict response schemas need to accept the additions. Admin creation intentionally stops returning entity-only fields.
- Lookup permissions expose only labels/IDs to users who need form/report selectors. List sizes should be assessed before adding server pagination.
