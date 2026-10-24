# React Phase 2A — UI polish and reusable management patterns

## Branch dependency

Work began from the latest fetched main, `bff3254`. Contrary to the task's assumption, React Phase 1 was **not merged**: PR #4 was still open and main contained no frontend. The Phase 1 commit `7118465` was fast-forwarded onto the new `feat/react-phase-2a-ui-polish` branch before these UI changes. Neither PR was merged. The Phase 2A PR targets main and depends on #4; until Phase 1 lands, its main-relative diff also includes Phase 1. If #4 is squash-merged, rebase Phase 2A's UI commit onto the resulting main before merging this PR.

## Design goals and result

Use the supplied fleet/rental/inventory references as inspiration for information hierarchy, grouped navigation, KPI presentation and table controls, without copying their branding, layouts or unsupported metrics. MUI remains the only component library.

The primary light experience combines deep navy navigation, slate text, a cool neutral canvas, white bordered surfaces and restrained teal accents. Dark mode uses separate surface/text/border tokens. Success/warning colors are semantic and limited. The login background is now solid navy rather than a gradient. No animation or decorative imagery was added.

Theme definitions moved to `src/app/theme.ts`. The eight-pixel spacing system, compact typography hierarchy, 12px surface corners and smaller control corners establish consistent density. Component overrides cover buttons, cards, papers, chips, text fields, tables, drawers, app bar, tooltips and icon buttons. The top bar separates workspace context, theme control, identity and logout. The sidebar groups permitted modules into Workspace, Operations, Commercial and Administration; empty groups disappear. Icons, active-state styling and `aria-current` distinguish navigation. Mobile uses a dismissible drawer with a close button and closes after route selection. A keyboard skip link targets the main content.

Dashboard sections continue to use the existing `/api/dashboard` and its exact 12 count fields. No revenue, utilization, trends, percentage changes or date-period interpretations were invented. KPI cards add labels, numeric hierarchy, subtle category icons and explanatory text; only the first card in each section has an accent border. A refresh button uses the existing query. Missing counts remain em dashes, empty responses have a helpful empty state, and existing ProblemDetail normalization remains authoritative.

## Reusable component system

| File | Components / responsibilities |
| --- | --- |
| `src/shared/ui/Layout.tsx` | `PageHeader` with eyebrow/title/description/action slots; `SectionCard` with header/body boundaries; `StatCard` with value/supporting text/icon. |
| `src/shared/ui/Filters.tsx` | Controlled accessible `SearchField`; `TableToolbar`; wrapping `FilterToolbar` with chip/clear/more-filter slots. |
| `src/shared/ui/DataTableShell.tsx` | Generic typed `Column<T>`/`DataTableShell<T>` with column renderers, row identity, toolbar/filter/empty slots, horizontal scrolling and controlled pagination; `StatusChip`; controlled `ConfirmDialog`. |
| `src/shared/States.tsx` | Reusable `EmptyState`, `LoadingState` (existing `Loading` alias retained), `ErrorState` and `AccessDenied`; HTTP 403 does not log the user out. |
| `src/app/AppShell.tsx` | Responsive application frame, grouped permission-aware navigation, identity, theme control and breadcrumbs. |

`DataTableShell` intentionally does not fetch, filter or paginate server records itself. Its caller supplies the visible rows and total count plus controlled pagination callbacks; this supports future server pagination without implying that any existing backend endpoint supports it. Filters and table toolbars are independent composition slots. Status chips include visible text; color alone does not communicate state. The table scroll region is keyboard-focusable. Confirmation remains a controlled presentation component; no mutation is connected.

## Management page pattern

The permitted Customers route demonstrates `ManagementTemplate`:

1. PageHeader with title/description and a disabled “Add customer” action showing future action placement.
2. A persistent notice explaining that the module is a layout preview.
3. TableToolbar with an accessible company/reference search field.
4. Status filter chip, clear-filter action and an optional right-side filter drawer.
5. DataTableShell containing either an explanatory empty state or explicitly opted-in sample rows.
6. Text status chips, row action menus and rows-per-page controls.

It starts empty. “Show sample layout”/“Preview sample rows” reveals three deliberately named **Example company** rows with **EXAMPLE** references. These are local presentation fixtures, not customer DTOs or API data. Search/status controls operate only on those fixtures. View/edit menu entries are disabled; the enabled menu entry explains the sample in a dismissible dialog. There are no create/edit/delete requests, forms, saved filters, live customer counts or business mutations. Other future modules retain polished planned-module states. Existing permission guards still protect direct URLs.

For Phase 2B, replace local fixtures with an explicitly reviewed endpoint contract, keep UI state in the page/feature, use TanStack Query for data, map ProblemDetail field errors, and add distinct write/transition permissions. Read/navigation permission must not grant create/edit/delete access (OPERATIONS, for example, may view customers but not mutate them).

## Preserved behavior and scope

AuthProvider, token/session handling, Axios client, centralized permission model, Vite proxy and backend source are unchanged. No frontend dependencies were added or upgraded, no second UI library, and no backend/schema modifications. Phase 1's reload-requires-login behavior remains. Role filtering and backend authorization remain authoritative.

## Verification executed

From `frontend/`:

```sh
npm ci --cache /workspace/.npm
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
```

Installation succeeded using the existing lockfile. TypeScript, ESLint and Prettier checks passed. **32 tests passed**: the existing 24 authentication/role/API/dashboard tests plus eight new layout/template interaction tests. New checks cover grouped navigation/current-page/breadcrumbs/identity, actual theme toggling without logout, mobile drawer open/close/route selection, honest empty/disabled-action states, sample search/status/clear controls, filter panel behavior, non-mutating row menus/dialogs, and pagination controls. MSW rejects unhandled requests; template interaction tests run without business API handlers. The test harness defaults matchMedia to a desktop viewport and explicitly selects mobile for drawer tests. Production build passed; upstream Zod annotation warnings and existing dependency deprecations are unchanged from Phase 1.

Additional browser validation used temporary Playwright tooling outside the repository with installed Chromium and intercepted API fixtures, **not a live backend**. Desktop 1440×1050 and mobile 390×844 checks exercised login, navigation, theme switching, sample layout and filter panel. No page errors or mobile page-level horizontal overflow were observed. Screenshots were inspected for light/dark contrast, card spacing and responsive layout. An initial laptop KPI grid was tightened after visual inspection. These are smoke/visual checks, not a full accessibility audit or cross-browser certification.

### Review screenshots

All identity and dashboard counts below are browser-test fixtures. They are not seeded application records.

- [Desktop dashboard, light](images/phase-2a/dashboard-light.png)
- [Management template, light](images/phase-2a/customers-light.png)
- [Management template, dark](images/phase-2a/customers-dark.png)
- [Mobile dashboard](images/phase-2a/dashboard-mobile.png)

## Files changed relative to Phase 1

- Added `frontend/src/app/theme.ts` and `AppShell.tsx`; simplified `App.tsx` to routing/providers.
- Added the three `frontend/src/shared/ui/` component files above.
- Added `frontend/src/features/management/ManagementTemplate.tsx`.
- Updated dashboard presentation, shared states and login background.
- Updated test setup and added `frontend/src/test/ui-polish.test.tsx`.
- Added this report and four review screenshots under `docs/images/phase-2a/`.

## Limitations and recommended next work

- PR #4 must land first; see the branch-dependency note above.
- The table demonstrates layout and client-side sample filtering only. Selection, sorting, exports, real pagination and mutations require separate endpoint and UX decisions.
- Disabled actions and planned-module labels are intentional until approved CRUD is implemented.
- ConfirmDialog is a reusable shell, not authorization or mutation logic. Destructive actions need contextual wording and pending/error handling in their owning feature.
- Theme preference remains memory-only. Browser checks used Chromium and mocked API responses; live integration, other browsers, screen-reader and contrast audits remain rollout work.
- Phase 2B candidates: customer directory, crane inventory, then quotations, each with read/write permissions, validation, lookup integration, error recovery and endpoint-specific tests. Scheduling/job workflows should follow after these foundations are verified against live backend behavior.
