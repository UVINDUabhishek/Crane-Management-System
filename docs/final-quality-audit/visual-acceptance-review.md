# Visual and functional acceptance review - 2026-11-11

## Environment and safety

Reviewed the root CHMS_Final_Quality_Audit.md and the previous audit README in full. Work stayed on checkpoint/chms-local-20261111 at HEAD 6f85c172fdbe2f96e6b61fcbee605e028bd65d68. Existing staged and unstaged changes were preserved. No branch operations, commits, resets, merges or pushes.

Real browser: ChatGPT in-app Chromium browser, using the actual Vite frontend and Spring Boot backend. A temporary Java launcher under ignored target/acceptance started PostgreSQL 17 through Testcontainers, using a separate chms_acceptance database and backend port 18080. Frontend port 5173 proxied only to that backend. Seeded five test-role accounts and used synthetic acceptance.test business data. No application database was used. Test accounts, records and containers were disposable; after the browser work the acceptance processes were stopped and docker ps showed no containers remaining.

## Pages and viewport coverage

Screenshot inspection plus DOM geometry covered Overview, Cranes, Customers, Quotations, Job cards, Timesheets, Maintenance, Invoices, Reports and Users at each width: **375, 768, 1280, 1440 CSS pixels**. Heights were 812, 1024, 900 and 1000 pixels respectively. Login was checked at all four widths; Operators was inspected through its administration tab at all four widths. These are emulated browser viewports, not physical mobile devices.

No horizontal document overflow was measured in the 40 main page/viewport combinations. Wide tables intentionally retain local horizontal scrolling. Long synthetic customer names were used in directories, dropdowns, forms and invoice details. Native table scrolling, wrapped pagination, responsive drawer navigation, sidebar logo and spacing were inspected. Dashboard cards changed from four columns to two and one as space narrowed.

Light-theme page coverage was comprehensive for the above views. Dark mode was additionally inspected on tablet/mobile Overview and the mobile drawer; this was not a second complete matrix in dark mode. No redesign was necessary.

Raw measurements and genuine, sanitized screenshots: [evidence directory](visual-acceptance/). Saved examples include [desktop invoice alignment](visual-acceptance/invoices-1440.jpg), [tablet users](visual-acceptance/users-768.jpg), [mobile reports](visual-acceptance/reports-375.jpg), [corrected validation](visual-acceptance/crane-validation-1440.jpg) and [service error](visual-acceptance/report-error-1440.jpg).

## Workflows actually exercised

| Area | Browser actions and observed results |
| --- | --- |
| Authentication | Empty-field validation, rejected password with preserved login form, successful login and logout for ADMIN, SALES, OPERATIONS, FINANCE and MAINTENANCE. Navigation matched role permissions. |
| Cranes | Empty form validation, create synthetic 080T crane, directory and details/control layout. Rechecked corrected numeric error text. |
| Customers | Create synthetic company with a long name; directory/layout review. Operations account had View access with no Add/Edit controls. |
| Quotations | Create quotation using customer and existing crane-type lookups, date entry and pricing. |
| Job Cards | Create pending job; completion without Timesheets rejected with the real server conflict. Cancel restored focus to the action trigger. Complete after recording hours succeeded. |
| Timesheets | Pending-parent lookup, structured Job Card/customer option on mobile, create, edit hours from 6 to 7, confirmed delete, verify empty state, recreate. Terminal-parent directory retained View without write actions. |
| Maintenance | Create scheduled service, Start to IN_PROGRESS, Complete to COMPLETED. Terminal Edit disappeared while authorized View Details remained, including for MAINTENANCE role. |
| Invoices | Job completion generated INV-0001 at 8,500 (existing 8-hour minimum x 1,000 plus 500 mobilization). View details, Complete, then Revise produced INV-0002 / Revision 1; original marked Revised, new record Draft. Finance account could view the revision. |
| Reports | Run live Job Cards General report; completed job showed 7 worked hours. Initial empty/report-selection state inspected. After stopping only the disposable backend, Run and Retry displayed a sanitized service-unavailable error while keeping filters usable. |
| Administration | Users directory with all five roles; Operators tab and empty state; tab and responsive layout inspection. Account creation was handled only by the disposable launcher; password changes and all user-administration mutations were not browser-exercised. |

Additionally, **45 live API checks** (five accounts x nine read endpoints) matched the expected 200/403 matrix. See [API role checks](visual-acceptance/api-role-checks.json). This complements rather than substitutes for the backend test suite.

Loading/pending controls were observed during actual sign-in and save requests. They were not tested under every artificial latency condition. Empty states, required-field validation, real lifecycle conflict feedback, confirmations, populated states and backend-unavailable errors were inspected.

## Accessibility and interaction checks

- Job completion cancellation returned focus to its originating action button.
- Action menu opened with Enter; Escape closed it and restored focus to the More Actions trigger.
- Dialog Tab/Shift+Tab wrapped between the first input and final action; Escape closed the form and returned focus to Add crane.
- Mobile drawer Escape returned focus to Open navigation with a visible focus ring.
- Focusable table scroll region accepted ArrowRight; measured scrollLeft increased to 120px. Native keyboard scrolling was preserved.
- Popup option accessible names retained both job number and customer identity. Numeric errors are field-specific and associated with their inputs.

These checks are not a formal WCAG conformance audit or screen-reader certification.

## Confirmed defects and targeted fixes

1. **Technical numeric validation text:** blank Capacity/Year exposed Zod's NaN diagnostic; fractional Year also used a generic diagnostic. craneSchema.ts now supplies clear numeric and integer error messages. Required/positive/integer constraints and backend rules are unchanged. Added three schema regressions and confirmed the blank-form text in the live browser.
2. **Actions heading alignment:** the heading was right-aligned while the stable icon grid began on the left of a variable-width action cell. DataTableShell now gives only the actions column a 172px minimum/header width (three 44px slots, two 4px gaps and cell padding) and left-aligns its heading. Numeric columns preserve their alignment. Added a regression for rendered text alignment and width. Rechecked multiple permission combinations in invoice/user rows and the responsive matrix.

No additional confirmed significant functional defect was found in the exercised workflows. No production backend or financial logic was changed.

## Automated verification

- Full frontend: `npm run test -- --reporter=verbose` -> **316 passed / 316**, 9 files passed, 0 failed, 0 skipped, 581.16 seconds. Four added regression cases explain the increase from 312.
- Affected crane cases passed in the focused run; the final action-control regression run passed 7/7. An initial new test asserted an HTML align attribute, but Material UI uses CSS classes; corrected it to verify rendered text alignment while retaining the width and numeric-column assertions.
- `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run build` -> passed. The build retained harmless dependency-only Zod annotation warnings.
- `git diff --check` -> passed. A final comparison against per-file snapshots from the start of this pass confirmed changes only to the two intended production frontend files, their regression tests, and acceptance documentation/evidence. The original eight-file staged diff remains unchanged (68 additions / 20 deletions).
- Node 24.22.2 was invoked from its existing NVM install. Test execution required the unsandboxed runner; no timeouts, assertions, test thresholds or dependencies were relaxed.
- Exact focused commands: `npm run test -- src/test/cranes.test.tsx src/test/rowActions.test.tsx` and `npm run test -- src/test/rowActions.test.tsx --reporter=verbose`.
- Live server-side RBAC probe: 45/45 expected responses. Temporary launcher, scripts and logs remain ignored under target/acceptance/. Browser evidence in this report contains only synthetic test data.

Existing local Maven XML reports were inspected: **562 tests, 0 failures, 0 errors, 0 skipped**. JaCoCo totals: instructions 6151/6254 (98.35%), branches 273/310 (88.06%), lines 1596/1608 (99.25%), methods 320/339 (94.40%), classes 47/47 (100%). The user's successful clean verify is consistent with these artifacts. Backend verification was not rerun during this frontend-only acceptance pass.

## Remaining limits and manual follow-up

- Chromium only; Firefox, Safari, real touch devices, browser zoom/accessibility settings, forced colors and screen readers were not independently tested.
- Dark mode was sampled, not exhaustively repeated across every page and viewport.
- Not every report type or every permitted/forbidden mutation was repeated in the browser; those broader cases remain supported by automated tests. Concurrent multi-user mutation timing was not manually reproduced.
- Retry rendering was verified while the acceptance backend remained stopped; browser recovery after restoring the backend was not separately exercised.
- Native date inputs needed the browser accessibility setValue control; generic automation fill did not populate date segments. Actual date entry and saving succeeded; no application date defect was inferred from the tooling limitation.

For additional manual acceptance: repeat the relevant workflows in Firefox/Safari and on a real touch device; check keyboard/reader announcements and zoom; repeat all pages in dark mode; then use two disposable sessions for concurrent state changes.
