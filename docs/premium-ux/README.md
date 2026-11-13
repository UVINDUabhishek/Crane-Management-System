# Enterprise workflow UX refinement

Implemented on `feat/premium-enterprise-ux`, based on verified delivery `0b9297224f8c56c0834241bba7ab19bbc6f0ac41`. The completed feature branch, backend, API contracts and existing stashes were not modified. No push or merge performed.

## Presentation changes

- **Shell:** global header now identifies the workspace section rather than repeating the page title. Compact page headings, narrower desktop navigation, reduced content/header spacing and a constrained sidebar scroll region. Existing theme toggle, account actions, role filtering, skip link and mobile drawer remain.
- **Job Cards:** singular Add Job Card action; server-derived total/pending/completed/canceled summaries; directory prioritizes reference, customer, crane, operator, schedule, worked/billable hours and status. Search, status filters, reset, pagination and role/lifecycle-aware action menus. Assignment, schedule and pricing detail sections replace a flat response dump. Completion retains invoice-generation guidance; creation explains salesperson and optional operator behavior.
- **Quotations:** customer/commercial reference, validity date range, minimum hours/rate and mobilization columns. Pricing and schedule form groups; IDs and description remain available in details. Existing editing/deactivation behavior retained.
- **Timesheets:** job/customer/date/hours directory. Job selector includes customer context and offers pending jobs for new entries. An existing selected record remains representable; the backend remains authoritative for terminal-job restrictions.
- **Maintenance:** crane, maintenance type, schedule, assignee and status directory. Contextual Start/Complete/Cancel actions preserve existing role/state checks. Optional assignees display a neutral dash.
- **Invoices:** customer/reference/date/total/status/version directory; financial values have separators and two decimal places. Detail sections retain quantity, rate, mobilization and total; guidance explicitly says total already includes mobilization. Draft editing, completion, cancellation and revision endpoints are unchanged; no manual creation added.
- **Users/Operators:** accessible administration tabs with independent page state; existing password-change form and safe deactivation confirmation preserved. No password data is introduced into directory/detail rendering.
- **Reports:** report builder with contextual date/status inputs, named customer/crane/salesperson lookup selectors, manual historical ID support, server-returned summary KPIs and formatted result cells. Existing report permissions and endpoints remain authoritative.

`presentation.tsx` owns presentation-only directory definitions, formatting, metrics, action menus and field grouping. API requests, mutations, normalized errors, locks and cache invalidations remain in the existing workflow layer. No new application dependencies. Customers and Cranes retain their existing implementations and inactive-row styling.

## Verification

Executed on Linux with Node 24:

| Check | Result |
| --- | --- |
| `npm run test:doctor` | Passed; correct frontend directory/config, one worker, 15000 ms timeout |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run format:check` | Passed |
| `npm run test` | 149 passed, zero failed/skipped; 6 files; 76.69 seconds |
| `npm run build` | Passed; existing non-fatal Zod annotation warnings |

Backend tests were not rerun because backend source/configuration is unchanged. The original 144 tests are retained, with intentional UI query updates. Five additional tests cover summary/filter behavior, restricted menus/cancellation, named report lookup requests, administration tabs, and request-gated pending completion/refresh.

## Visual evidence

Screenshots in this directory were captured with system Chromium through Playwright using **synthetic intercepted API responses**. These are visual/layout checks, not live-backend acceptance, and do not modify developer or production data. No example data is included in application code.

- `jobs-1440-light.png` / `jobs-1440-dark.png`: desktop directory.
- `jobs-768-light.png` / `jobs-768-dark.png`: tablet layout.
- `jobs-390-light.png` / `jobs-390-dark.png`: mobile layout with contained, keyboard-focusable table scrolling.
- `job-details-dark.png`, `invoices-dark.png`, `reports-dark.png`: representative detail, finance and reporting views.

Automated checks found no document-level horizontal overflow at 1440, 768 or 390 pixels in either theme and no JavaScript page exceptions. Initial browser automation used an overly strict accessible-name matcher for the report selector; corrected to match the actual browser-computed name. The initial unit run also exposed ambiguous status matches and modal visibility timing in updated tests; assertions now target the table and wait for dialog closure where appropriate.

## Remaining limits

- No currency code/symbol is configured in the existing frontend contract. Amounts use consistent numeric formatting without inventing a currency; confirm the intended currency before adding labels.
- Quotation/user responses still lack active flags. The UI retains their existing deactivation workflow and does not fabricate activation state.
- Historical inactive relationships may not appear in active-only lookup lists. Reports retain manual ID entry for those records.
- Tables use existing client-side pagination; narrow displays scroll within table regions. Exhaustive device/screen-reader testing and every CRUD permutation were not performed.
- No new live PostgreSQL/backend acceptance run in this redesign. Prior acceptance remains documented separately; frontend mocked tests and browser screenshots are explicitly distinguished here.
- No speculative utilization percentages, comparisons, or charts added. Reports render returned summary metrics and detailed records.
