# React Phase 2B-1 — Customer management

## Implementation and verified contract

Based on latest main `0c30a46` (merged Phase 2A). Inspected customer controller, service, entity, DTOs, repository, security, ProblemDetail handlers and customer integration tests before implementation. The supplied contract matches source. Main also includes direct per-icon imports; those are preserved.

The Customers route now loads the real API, with a client-side directory, detail view, shared create/edit form and explicit deactivation confirmation. Search is trimmed and case-insensitive across company, registration, VAT, contact person/number and email. A contact-email availability filter uses actual response fields. Pagination slices the returned array, resets on filters and clamps after refresh. Nullable values display placeholders. No preview fixtures remain in application code.

| Operation | Existing endpoint | Success |
| --- | --- | --- |
| List | GET `/api/customers` | 200, plain array |
| Detail | GET `/api/customers/{id}` | 200, object |
| Create | POST `/api/customers` | 201, object |
| Edit | PUT `/api/customers/{id}` | 200, object |
| Deactivate | DELETE `/api/customers/{id}` | 204, empty body |

The shared Axios base is `/api`; feature calls use `/customers` without query parameters. Responses contain numeric `id`, `companyName`, `registrationNo`, `vatNo`, `companyAddress`, and nullable `billAddress`, `contactPerson`, `contactNo`, `customerEmail`. Create/update payloads contain only the eight business fields, without id, active or metadata.

Required values are trimmed nonblank strings; contact number must match `^0\d{9}$` and remains a string. Optional email is trimmed, validated when present, and submitted as an empty string when blank (accepted by backend @Email). No invented length/registration/VAT constraints. Known ProblemDetail field errors map to form fields; a normalized error summary also covers unknown fields. Failed submissions preserve edits. Pending requests block duplicate submission and dismissal. Query invalidation refreshes directory and selected detail; detail cache is discarded after closing to fetch fresh data on reopening.

## Authorization and limitations

ADMIN and SALES can read, create, edit and deactivate. OPERATIONS can read the directory/details only. FINANCE and MAINTENANCE retain route denial and issue no full-customer requests. `canWriteCustomers` separates mutation access from the existing read permissions. Backend 403 responses are still handled. AuthProvider, protected routing, session expiry/cache clearing, Axios interceptors, Vite proxy and backend are unchanged.

DELETE soft-deactivates; the list includes inactive records, responses omit `active`, and no reactivation endpoint exists. Confirmation explains these limits. Success invalidates queries but does not remove the row or invent durable status. No activation badges, status filters, status payloads or reactivation action exist. A reliable status response/reactivation contract is a separate backend follow-up.

All search/filtering/pagination is local to the full returned array. Large directories will eventually need an explicitly designed server pagination contract. No optimistic updates or second cache were introduced. Phase 2B-2 Crane Management is not included.

## Files changed

| File | Purpose |
| --- | --- |
| `frontend/src/features/customers/customersApi.ts` | Typed existing REST calls and query keys. |
| `frontend/src/features/customers/customerSchema.ts` | Zod schema, request values, field labels and nullable defaults. |
| `frontend/src/features/customers/CustomersPage.tsx` | Directory, filtering/pagination, role actions, modal handoff, deactivation and feedback. |
| `frontend/src/features/customers/CustomerFormDialog.tsx` | Shared create/edit form, pending protection, errors and invalidation. |
| `frontend/src/features/customers/CustomerDetailsDialog.tsx` | Fresh detail query, grouped fields, loading/retry/not-found and edit loader. |
| `frontend/src/features/management/ManagementTemplate.tsx` | Removed obsolete preview screen. |
| `frontend/src/app/App.tsx` | Route now renders CustomersPage. Protected is unchanged. |
| `frontend/src/app/AppShell.tsx` | Removed Customers' Planned badge only. |
| `frontend/src/shared/permissions.ts` | Separate customer write helper. |
| `frontend/src/shared/ui/DataTableShell.tsx` | Optional ConfirmDialog feedback slot, so mutation failures are visible inside the modal. |
| `frontend/src/test/customers.test.tsx` | 45 customer schema/API/UI regression cases with real providers and MSW. |
| `frontend/src/test/ui-polish.test.tsx` | Kept shell/theme/mobile tests; replaced five obsolete preview tests with customer coverage. |
| `frontend/src/test/refinement.test.tsx` | Kept shared-component tests; replaced preview menu/focus case with actual customer detail/focus coverage. |
| `docs/frontend-phase-2b1-customer-management.md` | This implementation/verification report. |
| `docs/images/phase-2b1/directory-{390,1440}-{light,dark}.png` | Four browser directory screenshots, fixture data. |
| `docs/images/phase-2b1/form-{390,1440}-{light,dark}.png` | Four browser form screenshots, fixture data. |

## Verification executed

From `frontend/` (cache flag selects a writable local cache):

```sh
npm ci --cache /workspace/.npm
npm run format
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
npm audit --audit-level=moderate --cache /workspace/.npm
```

All commands passed. **93 tests run, 93 passed, 0 failed, 0 skipped**, across four files. The suite retains 48 applicable Phase 1/2A tests and replaces six obsolete preview cases with 45 customer cases. Coverage includes all roles, denied direct access, list/detail/POST/PUT/DELETE contracts, bearer header, query refresh, nullable values, six-field search, email filter, pagination/reset/clamping, loading/empty/retry, form validation, pending protection, normalized failures, field errors, retained edits, focus return, 404 detail, and deactivation/session-expiration behavior. MSW rejects unhandled requests. No component implementations were mocked.

TypeScript, ESLint, Prettier and production build passed. Audit: **0 vulnerabilities**. Existing ESLint and whatwg-encoding deprecation notices and upstream Zod Rollup annotation warnings remain; none were suppressed. No dependencies changed. Backend tests were not rerun because backend files are unchanged.

### Browser review

Real Chromium with temporary Playwright tooling exercised **8 combinations**: 390, 768, 1366 and 1440px, light and dark themes. Every combination exercised login/navigation, directory pagination/search, detail, edit, deactivation and creation. There were no page errors or page-level horizontal overflows. Eight screenshots were produced; desktop directory and mobile dark form were visually inspected. The table scrolls within its container on narrow screens.

All browser API responses were intercepted test fixtures. Spring Boot was not listening on localhost:8080; no live-backend success is claimed. Screen-reader and cross-browser/device testing remain outstanding. The existing reduced-motion and Material UI dialog behavior are retained; menu actions wait for exit before opening a dialog.

[Light directory](images/phase-2b1/directory-1440-light.png) · [Dark directory](images/phase-2b1/directory-1440-dark.png) · [Mobile directory](images/phase-2b1/directory-390-light.png) · [Mobile form](images/phase-2b1/form-390-dark.png)

## Live-backend manual verification

1. Use a non-production PostgreSQL database at `localhost:5432/crane_management_system` with the configured postgres user. Supply `DB_PASSWORD` and `JWT_SECRET` securely in the shell/environment; do not commit them. With Java 21, start Spring Boot from the repository root using `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). Confirm it listens on localhost:8080. Use existing authorized test accounts; this change creates no credentials.
2. In `frontend/`, run `npm ci` then `npm run dev`. Open `http://localhost:5173`. The existing proxy forwards `/api` to port 8080. Do not serve the build without configuring an equivalent proxy.
3. Sign in as ADMIN, open Customers, and create a clearly named test company with unique registration/VAT strings, both addresses, contact person, and a number such as `0771234567`. Leave email blank, then repeat validation with an invalid phone/email and blank required fields. Inspect Network for POST `/api/customers`, 201, and only the eight business fields.
4. View details (GET by ID), edit an address (PUT by ID), close/reopen detail and confirm the saved value. Try a duplicate registration to check conflict feedback and retained form values. Search across company, registration, VAT, contact and email; toggle Has contact email. With more than five test records, change pages and page size, then search to confirm reset.
5. Open Deactivate, cancel once and confirm no DELETE occurs. Reopen and confirm; expect DELETE 204 and accurate success feedback. The row should remain because GET includes deactivated records. There must be no status badge/filter or Reactivate action. Use browser throttling to verify disabled pending controls and retained errors on a failed request.
6. Repeat read/write flows as SALES. Sign in as OPERATIONS: directory/detail must work, with no Add/Edit/Deactivate actions. FINANCE and MAINTENANCE must lack Customers navigation; navigating directly to `/workspace/customers` must show Access denied without a customer request.
7. Verify logout, expired-token return to login, both themes and narrow-screen navigation. Unexpected backend 403 should show denial without a false success. Review the supplied screenshots as layout references, not proof of live integration.

The feature is ready for draft PR review. Live deployment acceptance should include the above checks and assistive-technology review. No merge or next module is authorized by this delivery.
