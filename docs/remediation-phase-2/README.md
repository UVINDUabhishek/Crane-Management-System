# Final remediation — Phase 2: relational selectors and validation

Continues Phase 1 (`344c5c20758bee868d8ef9bc3b29979a1e399238`) on `fix/chms-final-remediation`. Scope is the supplied Phase 2 request. All Phase 2 changes remain uncommitted at the user’s instruction. No report or action-column changes, database migrations, push or merge.

## Implemented behavior

- Workflow relation fields use searchable MUI Autocomplete with named labels. Options are selected objects with stable IDs; requests retain their original numeric ID fields. Free-text option creation is disabled. Optional clearing produces null.
- Lookup requests are deduplicated by React Query keys, fetched only for an open form, and cached for 30 seconds. Loading, no eligible records, normalized error and retry states are available. No per-row/detail lookups were introduced.
- Job quotation options are restricted to the selected customer (or the persisted customer during editing, where customer is not an editable field). Customer changes clear incompatible quotations. Submission checks active quotation/crane availability, exact `type` compatibility and quotation validity coverage. Start/end ordering accepts equal dates.
- **Finite job end date:** the backend DTO permits null, but `JobCardService.validateQuotation` rejects it. The form explicitly requires an end date and explains why. No open-ended booking semantics were added. This representation/business-rule discrepancy remains for later review.
- Timesheet creation offers pending jobs only, labeled by job number/customer. Create and edit reject future work dates. The form section is `Hours`, and descriptions are full-width four-row textareas. Existing server duplicate checks and PENDING-only create/update/delete restrictions remain intact.
- Maintenance uses crane/person lookups and date-order validation. New maintenance-person options exclude ADMIN while retaining all other active roles. Historical assignments omitted from active-only lists remain visible by their saved name, marked `current assignment`, and their original IDs remain in the form unless explicitly changed or cleared.
- Quotation creation uses the existing customer-name lookup. The quotation update DTO does not accept `customerId`; edit displays the saved customer read-only rather than implying reassignment is supported. Job edits likewise retain their customer, and timesheet edits show their immutable job-card reference. `Crane type / rated capacity` options use actual active crane **type strings**, never numeric capacity. Existing saved types remain displayable on edit. Validity allows equal dates; descriptions have full-width multiline space.
- Existing form validation, request-field allowlisting, normalized server errors, pending locks, cache invalidations and role permissions are retained. Recoverable errors do not reset entered values.

## Minimal backend/API changes

| Endpoint | Response change |
| --- | --- |
| `GET /api/lookups/cranes` | Existing `id`, `label`; adds `type` via `CraneLookupResponse` |
| `GET /api/lookups/quotations` | Existing `id`, `label`, `customerId`; adds `type`, `validFrom`, `validTo` |
| `GET /api/lookups/maintenance-people` | Same `{id,label}` schema; active non-ADMIN roles only |

Metadata is necessary for permitted OPERATIONS users to validate bookings without calling unauthorized full crane/quotation endpoints. Queries remain projections with database filtering and deterministic ordering. No prices, emails, passwords or unrelated relationships are exposed. Existing fields and endpoint permissions are preserved.

`TimesheetService` rejects future dates on both creation and update with HTTP 400 / `INVALID_REQUEST`, including direct REST requests. The frontend and service both use the **Asia/Colombo business date**, not UTC date truncation or the browser/host timezone. This matches the current Sri Lankan operating context. There is no configurable tenant timezone in this phase; confirm the operating timezone before deployment to another region. Date-only values are transmitted unchanged as `YYYY-MM-DD`.

Maintenance filtering affects new UI selections, not historical data: the existing service still permits retaining a previously assigned admin. Active-only lookup omissions do not silently null optional assignments. Job validation continues to require active customer/quotation/crane records, as before.

## Changed code

- `frontend/src/features/workflows/WorkflowPage.tsx`: passes form context/initial data, relation validation, dependent clearing, multiline layout and timesheet section naming.
- `frontend/src/features/workflows/formRelations.tsx`: shared lookup fetching, selectors, historical display, local-date computation and dependent/date validation.
- `frontend/src/test/relations.test.tsx`: focused selector and validation regression coverage.
- Backend `LookupController`, `LookupService`, `CraneRepository`, `QuotationRepository`, `CraneLookupResponse`, `QuotationLookupResponse`: minimal lookup metadata and eligibility changes.
- `TimesheetService`: future-date guard while preserving lifecycle/duplicate checks.
- `LookupIntegrationTest`, `TimesheetIntegrationTest`, `TimesheetServiceTest`: eligibility, metadata, projection, REST persistence and future-date tests.
- `FrontendContractIntegrationTest`, `FrontendWorkflowIntegrationTest`, `ValidationStateIntegrationTest`, `integration/support/IntegrationTestDataFactory`, and `util/TimesheetTestDataBuilder`: formerly future timesheet dates moved to historical dates so existing workflow/duplicate/lifecycle tests remain valid under the new rule. No production records changed.

## Verification

Final frontend verification on November 1, 2026 (Asia/Colombo), Linux / Node 24:

| Check | Result |
| --- | --- |
| `npm run test:doctor` | Passed; expected frontend directory/configuration |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run format:check` | Passed |
| `npm run test` | 178 passed, zero failed/skipped; 7 files; 95.15 seconds |
| `npm run build` | Passed; existing non-fatal Zod annotation warnings |
| Maven `clean verify` | Previously executed for these backend changes: 555 tests passed, zero failures/errors/skips; BUILD SUCCESS |
| JaCoCo | All existing thresholds passed; 47 classes analyzed |

Maven ran with JDK 21, Docker/PostgreSQL Testcontainers and a separate disposable application-context database. Backend source/tests were checked for changes after that successful run: none. It was therefore not repeated during resumption. The resumed frontend run includes the final read-only relationship regression and preserves all 162 Phase 1 tests (16 additional cases). No checks or tests were disabled.

Frontend coverage includes dependent filtering/clearing, exact ID payloads, same-day dates, missing end date, type mismatch, invalid ranges, today/future dates on create and edit, business-date midnight boundaries, historical assignments, empty optional fields, real crane-type values, keyboard selection, no eligible options, lookup retry, pending/error retention, and read-only quotation/timesheet edit context with exact update-only payloads.

Backend tests include all-role lookup permissions, exclusion of ADMIN/inactive maintenance choices with every other role retained, deterministic projection queries, new metadata, service-level future-date checks and real PostgreSQL REST create/update requests accepting business today and rejecting tomorrow without persisting it.

Early verification findings were corrected: a lookup projection test still counted the now-excluded admin; new form tests initially compared React Query's extra mutation context argument rather than just the submitted payload; the lookup-error test now expects the existing normalized 503 message. An unsupported RTL `exact` option was removed from the new selector test helper; string option names already match exactly.

## Browser evidence and limitations

Chromium/Playwright checked searchable/keyboard selectors and customer-to-quotation clearing at 1440, 768 and 390px; no dialog horizontal overflow or JavaScript page exceptions. `browser-results.json` and `job-form-*.png` record the checks. **Synthetic API fixtures were used**, not live browser/backend acceptance. Backend REST tests use isolated Testcontainers databases; a separate disposable database supplies the application-context test. No developer database was reset or changed.

Native Windows and exhaustive screen-reader/device acceptance were not executed. Clients should deploy with the additive lookup metadata backend changes. Changing a selection away from an unavailable historical assignment does not offer that record as a new option. Business review remains needed for nullable job end dates and any future configurable timezone policy. Stop at Phase 2.

## Resumed-work review

The interrupted run had already completed both read-only relationship controls and passed 177 frontend tests plus the build. Resumed work added the missing explicit timesheet edit regression (job number remains read-only and no IDs enter the update payload) and finalized this record. No additional application or backend changes were needed after resuming. The original 162-test Phase 1 baseline is preserved.

All actionable Phase 2 requirements are implemented. Relationship reassignment is deliberately unavailable where the backend update DTO excludes the ID: quotation customer, job customer and timesheet job. Supporting those changes would require a separately authorized backend contract/business-rule change. The finite job end-date discrepancy and non-configurable Asia/Colombo business timezone remain documented review items, not unfinished code. Reports and action-column work remain out of scope.
