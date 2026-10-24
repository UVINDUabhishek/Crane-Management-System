# Phase 0B-2 — Scheduling, validation, state protection and API errors

Base: `main` at `0b36560` (merged Phases 0A and 0B-1). Branch: `fix/phase-0b2-validation-scheduling`.

## Confirmed root causes

| Area | Source finding | Correction |
| --- | --- | --- |
| Reservations | Job writes did not query overlapping pending jobs; creation/update lacked a transaction or shared serialization point. | Database overlap predicate and transaction-scoped crane row locks shared with maintenance writes. |
| Job state | Updates accepted completed/canceled jobs; completion and timesheet changes could race. | Terminal updates/transitions return 409; all job and timesheet writes serialize on the parent job row. |
| Open-ended maintenance | SQL comparisons with null end dates failed to match overlaps; maintenance writes did not check existing jobs. | Inclusive, null-safe overlap predicates in both directions, applied on create/update/start. |
| Crane availability | Completing/canceling one maintenance record unconditionally released its crane. | Preserve MAINTENANCE while another IN_PROGRESS record exists; reconcile both cranes on in-progress reassignment. |
| Quotations | Jobs did not validate quotation customer, type, activity or validity dates. | Shared validation on create and every pending update, using the persisted job customer. |
| Validation/errors | Missing `@Valid`, invalid enum `@NotBlank`, generic domain RuntimeExceptions, and empty/plain-text security errors. | Request constraints, typed domain exceptions, application-wide ProblemDetail responses, and matching security-filter responses. |

## Reservation and transaction rules

- Only PENDING jobs reserve cranes. COMPLETED and CANCELED jobs retain their history and no longer block new reservations.
- Date ranges are inclusive: 10–14 and 14–20 conflict; 10–14 and 15–20 do not.
- A null job or maintenance end denotes an open-ended range. Existing open-ended pending jobs block future overlapping reservations until completion/cancellation. Null start dates and reversed date ranges are invalid.
- Only SCHEDULED and IN_PROGRESS maintenance participates in reservation checks. Completed/canceled maintenance never blocks future bookings, even with a null end.
- Job writes check the existing AVAILABLE crane status rule, maintenance overlaps, quotation compatibility, and pending-job overlaps. Reserving a crane does not mark it IN_USE.
- Active maintenance creation/update/start checks pending jobs before accepting its schedule. Maintenance-to-maintenance overlap remains allowed, as before.
- Update predicates exclude the current job ID. Rejected writes roll back relationships, dates, statuses, and any other changes.

`SchedulingService` centralizes the shared crane lock protocol and pending-booking check. Each mutation runs in one transaction. Existing job/maintenance mutations first lock their own row; affected crane IDs are then locked in ascending, distinct order, including both old and new crane IDs for reassignment. Creation locks the target crane before validation. PostgreSQL `SELECT ... FOR UPDATE` native repository queries lock only the target table row, avoiding locks over nullable eager associations. After a crane lock is obtained, its managed entity is refreshed because an eager job/maintenance association may have loaded it before the transaction waited.

At PostgreSQL's existing READ COMMITTED isolation, the overlap query runs after the crane lock is acquired and sees the preceding holder's committed changes. Locks remain held through save, automatic invoice creation where applicable, and commit/rollback. The overlap queries use database counts, not full-entity loading or Java-side filtering. Nullable date parameters have explicit date casts for PostgreSQL type inference.

Timesheet mutations first identify the parent with a scalar ID query, then lock the job row before loading/modifying the timesheet. Completion takes the same parent lock before reading timesheets or creating its invoice. Repeated/concurrent completion therefore cannot create two invoices, and timesheets cannot change billed totals after completion.

No database schema, indexes, dependencies, JWT generation, role permissions, or lookup contracts changed. No migration is required.

## Quotation compatibility and the unbounded-validity decision

New and updated job relationships require active customers, cranes and quotations. The selected quotation's customer ID must equal the persisted job customer ID, and its type string must exactly equal the selected crane's type. No type conversions or category mappings were introduced. Bounded job start/end must be within the quotation's inclusive validity window.

**Confirmation remains needed before adding unbounded quotation validity.** Both existing quotation request DTOs require `validTo`. A nullable database column alone does not define unbounded validity. This change preserves mandatory quotation dates and rejects open-ended job requests with finite or incomplete quotation validity (400). It does not silently give null quotation dates a new business meaning. Existing open-ended job records still receive correct reservation handling and can be completed/canceled; changing their schedule requires a compatible, bounded quotation/work period. Supporting new unbounded quotations can be enabled separately after confirming that policy.

Historical GET endpoints remain available when referenced entities are inactive. Compatibility checks apply to pending writes, including edits that would otherwise retain an incompatible existing quotation.

## State and maintenance history

COMPLETED and CANCELED jobs reject updates, completion and cancellation with 409. PENDING completion still requires timesheets, uses the same per-day minimum-hour billing, and creates exactly one DRAFT invoice in the transaction. Cancellation creates no invoice. Timesheet create/update/delete rejects terminal parent jobs with 409.

SCHEDULED maintenance reserves its date range without changing the crane's current status. Starting it sets MAINTENANCE. Completion/cancellation keeps that status if any other IN_PROGRESS operation remains; otherwise the existing AVAILABLE release behavior applies. Moving in-progress maintenance marks the new crane MAINTENANCE and reconciles the old crane against its remaining in-progress operations. Existing maintenance state-transition permissions remain intact; no new prohibition on editing terminal maintenance history was introduced.

No end dates are fabricated or overwritten on completion/cancellation. Existing maintenance reporting and dashboard historical calculations remain unchanged. Without a dedicated completion timestamp, a terminal record with a null end has incomplete duration history; this phase fixes active reservation eligibility rather than guessing its historical end date.

## API and validation changes

All URLs, success response DTO fields, authentication token format, roles and lookup response formats remain unchanged.

| Requests | Change |
| --- | --- |
| POST `/api/job-card`, PUT `/api/job-card/{id}` | Enforce reservation, quotation and active-reference rules; terminal PUT returns 409. |
| PATCH `/api/job-card/{id}/complete` or `/cancel` | Serialize transitions and invoice creation; terminal transitions return 409. |
| POST `/api/timesheets`, PUT/DELETE `/api/timesheets/{id}` | Serialize with parent state transitions; terminal parent mutations return 409. |
| POST `/api/maintenance`, PUT `/api/maintenance/{id}`, PATCH `/api/maintenance/{id}/start`, `/complete`, `/cancel` | Coordinate reservations, validate dates, and preserve other in-progress maintenance availability. |
| POST `/api/quotations`, PUT `/api/quotations/{id}` | Reject reversed validity dates (400); validity end remains mandatory. |
| POST `/api/auth/login` | Validate nonblank email/password and email format; failures return ProblemDetail. Successful raw JWT response is unchanged. |
| POST `/api/admin/users`, PUT `/api/admin/users/{id}` | Apply `@Valid`; roles use `@NotNull`, appropriate for an enum. |
| PUT `/api/admin/me/password` | Validate nonblank current/new passwords. No new password-length policy. |
| PATCH `/api/invoices/{id}/revise` | Apply `@Valid`; quantity/rate positive and required, optional mobilization nonnegative (zero allowed, consistent with invoice update). |

Additional DTO constraints: optional maintenance-person IDs and required timesheet job IDs must be positive. UpdateMaintenanceRequest now permits null `maintenanceEndDate`, consistently meaning ongoing maintenance. Existing optional timesheet hours/descriptions, optional operators/assignees, and other established constraints are retained. Quotation mobilization's existing positive-if-present constraint is unchanged.

For job updates, an **omitted** optional `endDate` retains its existing value. An **explicit JSON null** requests an open-ended work period and receives the same quotation validation as creation; with the current finite quotation contract it is rejected. This replaces the previous silent treatment of explicit null as “keep the old end”. The internal presence flag is JSON-ignored. No success DTO fields were added for it.

Known domain failures across user, customer, crane, operator, quotation, job, timesheet, maintenance and invoice services now use typed business exceptions. Changes outside scheduling are limited to validation and error categorization; pricing, invoice revisions, revenue calculations and Phase 0A contracts are retained.

## Error contract and frontend implications

Errors use `Content-Type: application/problem+json`:

```json
{
  "type": "urn:chms:problem:booking-overlap",
  "title": "Conflict",
  "status": 409,
  "detail": "Crane has a pending job during the selected period",
  "instance": "/api/job-card",
  "code": "BOOKING_OVERLAP"
}
```

Validation failures include `errors: [{"field":"email","message":"must be a well-formed email address"}]`. Rejected values, password hashes, tokens, Java exception types, SQL and stack traces are never included. The framework supplies `instance` for MVC responses; security filters set the request path explicitly. Unexpected failures are logged server-side and return only a generic 500 message.

| Status | Codes / meaning |
| --- | --- |
| 400 | `VALIDATION_FAILED` for bean validation; `INVALID_REQUEST` for malformed JSON/enums/types/date ranges/quotation incompatibility. |
| 401 | `UNAUTHORIZED`: missing/invalid/expired JWT, deactivated user, invalid login credentials. |
| 403 | `FORBIDDEN`: insufficient role, from either URL or method security. |
| 404 | `RESOURCE_NOT_FOUND`: missing resource or reference. |
| 409 | `BOOKING_OVERLAP`, `MAINTENANCE_OVERLAP`, `STATE_CONFLICT`, `DUPLICATE_CRANE`, `DATA_CONFLICT`, `CONCURRENT_MODIFICATION`. |
| 500 | `INTERNAL_ERROR`: unexpected failures only; arbitrary RuntimeExceptions are not reclassified as business errors. |

Other framework HTTP errors retain their status with `HTTP_ERROR`. Machine codes and type URNs are stable; clients should branch on status/code rather than parsing detail text. Display field errors from `errors`, treat 409 as a refresh/review conflict, and disable editing for terminal jobs. Authentication errors now have JSON bodies instead of empty or plain-text responses.

## Regression coverage and prior expectations

- SchedulingIntegrationTest: inclusive boundary/contained/enclosing/non-overlap cases; open-ended existing reservations; all bounded/open-ended maintenance combinations using PostgreSQL; terminal eligibility; different cranes; self-exclusion; reassignment persistence and rollback; invalid dates; maintenance status reconciliation and reassignment.
- Deterministic concurrency integration tests hold the shared DB row lock, wait until **both real HTTP transactions** are blocked in PostgreSQL, then release it. Cases cover concurrent job creation, job updates, maintenance/job creation, maintenance update/job creation, opposite crane reassignments, repeated completion, and timesheet creation versus completion.
- ValidationStateIntegrationTest: terminal state and timesheet mutation matrix; quotation customer/type/activity/validity/boundary checks on create and update; finite/open incompatibility; missing references; preserved omitted-end semantics; request constraints; sanitized 400/401/403/404/409 contracts; duplicate data; exactly one draft invoice and consistent billed hours.
- ApiFailureIntegrationTest injects unexpected and locking failures through an actual application endpoint to verify sanitized 500 and retryable 409 responses.
- SchedulingServiceTest verifies the lock order/refresh protocol, missing crane, overlap code and date validation.
- Existing service mocks now target transactional lock queries. Phase 0B-1's terminal-update cases were retained but changed to expect 409, as explicitly approved in this phase. Existing integration tests that expected servlet-wrapped runtime failures now assert the API's 409 response. The login unit test verifies delegation of authentication failures to the common handler; JWT filter tests use real mock HTTP responses to verify JSON output.
- No tests were deleted or disabled, and no coverage thresholds were lowered.

Final verification on Java 21 / Spring Boot 4.1.1, using PostgreSQL 17 Testcontainers:

```sh
./mvnw -s /workspace/.m2/settings.xml -Dmaven.repo.local=/workspace/.m2/repository -B -ntp clean verify
```

The settings/local-repository flags select this cloud environment's Maven cache; the full requested `clean verify` lifecycle was executed. **BUILD SUCCESS: 529 tests, 0 failures, 0 errors, 0 skipped** (72 new cases over the 457-test baseline). JaCoCo reported “All coverage checks have been met.”

| Counter | Covered / total | Coverage |
| --- | --- | --- |
| Instruction | 5984 / 6087 | 98.31% |
| Branch | 253 / 290 | 87.24% |
| Line | 1563 / 1575 | 99.24% |
| Method | 311 / 330 | 94.24% |
| Class | 44 / 44 | 100.00% |

Thresholds remain instruction/line/method 90%, branch 70%, class 100%. The earlier focused 70-test regression run also passed. `git diff --check` passed. No frontend or schema files changed.

## Remaining limitations

1. Unbounded quotation validity requires policy confirmation, as described above; it is not enabled by this PR.
2. The locking guarantee applies to application writes following this protocol. Direct SQL/imports or future writers must follow it; no PostgreSQL exclusion constraint was added.
3. Existing read-latest-plus-one document numbering can collide across independent cranes/jobs. Existing uniqueness constraints reject the losing transaction with 409 and roll it back. A separate numbering redesign is outside this phase.
4. Crane row locks serialize writes per crane and may wait under contention. Deadlock/lock-acquisition failures are returned as retryable conflicts. No arbitrary lock timeout or scheduling policy was introduced.
5. Quotation/customer metadata can still be edited independently after a job is reserved. Every subsequent pending job update revalidates compatibility; this phase does not freeze quotation snapshots or redesign related entity updates.
6. Historical terminal maintenance with no end date has no reliable completion timestamp; duration reporting remains limited by existing data. No historical records were rewritten.

## Files changed

### Production Java files

- [config/SecurityConfig.java](../src/main/java/com/uvindu/crane_management_system/config/SecurityConfig.java)
- [controller/AdminController.java](../src/main/java/com/uvindu/crane_management_system/controller/AdminController.java)
- [controller/AuthController.java](../src/main/java/com/uvindu/crane_management_system/controller/AuthController.java)
- [controller/InvoiceController.java](../src/main/java/com/uvindu/crane_management_system/controller/InvoiceController.java)
- [dto/ChangePasswordRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/ChangePasswordRequest.java)
- [dto/CreateInvoiceRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/CreateInvoiceRequest.java)
- [dto/CreateMaintenanceRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/CreateMaintenanceRequest.java)
- [dto/CreateTimesheetRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/CreateTimesheetRequest.java)
- [dto/CreateUserRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/CreateUserRequest.java)
- [dto/LoginRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/LoginRequest.java)
- [dto/UpdateJobCardRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/UpdateJobCardRequest.java)
- [dto/UpdateMaintenanceRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/UpdateMaintenanceRequest.java)
- [dto/UpdateUserRequest.java](../src/main/java/com/uvindu/crane_management_system/dto/UpdateUserRequest.java)
- [exception/ApiExceptionHandler.java](../src/main/java/com/uvindu/crane_management_system/exception/ApiExceptionHandler.java)
- [exception/ApiProblems.java](../src/main/java/com/uvindu/crane_management_system/exception/ApiProblems.java)
- [exception/BusinessException.java](../src/main/java/com/uvindu/crane_management_system/exception/BusinessException.java)
- [repository/CraneRepository.java](../src/main/java/com/uvindu/crane_management_system/repository/CraneRepository.java)
- [repository/JobCardRepository.java](../src/main/java/com/uvindu/crane_management_system/repository/JobCardRepository.java)
- [repository/MaintenanceRepository.java](../src/main/java/com/uvindu/crane_management_system/repository/MaintenanceRepository.java)
- [repository/TimesheetRepository.java](../src/main/java/com/uvindu/crane_management_system/repository/TimesheetRepository.java)
- [security/JwtAuthenticationFilter.java](../src/main/java/com/uvindu/crane_management_system/security/JwtAuthenticationFilter.java)
- [service/CraneService.java](../src/main/java/com/uvindu/crane_management_system/service/CraneService.java)
- [service/CustomerService.java](../src/main/java/com/uvindu/crane_management_system/service/CustomerService.java)
- [service/InvoiceService.java](../src/main/java/com/uvindu/crane_management_system/service/InvoiceService.java)
- [service/JobCardService.java](../src/main/java/com/uvindu/crane_management_system/service/JobCardService.java)
- [service/MaintenanceService.java](../src/main/java/com/uvindu/crane_management_system/service/MaintenanceService.java)
- [service/OperatorService.java](../src/main/java/com/uvindu/crane_management_system/service/OperatorService.java)
- [service/QuotationService.java](../src/main/java/com/uvindu/crane_management_system/service/QuotationService.java)
- [service/SchedulingService.java](../src/main/java/com/uvindu/crane_management_system/service/SchedulingService.java)
- [service/TimesheetService.java](../src/main/java/com/uvindu/crane_management_system/service/TimesheetService.java)
- [service/UserService.java](../src/main/java/com/uvindu/crane_management_system/service/UserService.java)

### Test Java files

- [controller/AuthControllerTest.java](../src/test/java/com/uvindu/crane_management_system/controller/AuthControllerTest.java)
- [integration/ApiFailureIntegrationTest.java](../src/test/java/com/uvindu/crane_management_system/integration/ApiFailureIntegrationTest.java)
- [integration/JobCardIntegrationTest.java](../src/test/java/com/uvindu/crane_management_system/integration/JobCardIntegrationTest.java)
- [integration/SchedulingIntegrationTest.java](../src/test/java/com/uvindu/crane_management_system/integration/SchedulingIntegrationTest.java)
- [integration/ValidationStateIntegrationTest.java](../src/test/java/com/uvindu/crane_management_system/integration/ValidationStateIntegrationTest.java)
- [integration/WorkflowIntegrationTest.java](../src/test/java/com/uvindu/crane_management_system/integration/WorkflowIntegrationTest.java)
- [security/JwtAuthenticationFilterTest.java](../src/test/java/com/uvindu/crane_management_system/security/JwtAuthenticationFilterTest.java)
- [service/CraneServiceTest.java](../src/test/java/com/uvindu/crane_management_system/service/CraneServiceTest.java)
- [service/JobCardServiceTest.java](../src/test/java/com/uvindu/crane_management_system/service/JobCardServiceTest.java)
- [service/MaintenanceServiceTest.java](../src/test/java/com/uvindu/crane_management_system/service/MaintenanceServiceTest.java)
- [service/SchedulingServiceTest.java](../src/test/java/com/uvindu/crane_management_system/service/SchedulingServiceTest.java)
- [service/TimesheetServiceTest.java](../src/test/java/com/uvindu/crane_management_system/service/TimesheetServiceTest.java)

### Documentation

- [phase-0b2-validation-scheduling.md](../docs/phase-0b2-validation-scheduling.md)
