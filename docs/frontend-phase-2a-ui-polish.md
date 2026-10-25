# React Phase 2A — Premium UI refinement and final hardening

## Branch and scope

PR #4 was squash-merged into `main` as `2b50d69`. Its tree matches the original Phase 1 commit `7118465`. The existing `feat/react-phase-2a-ui-polish` branch was reconciled by replaying only its Phase 2A commit onto that main commit. PR #5 remains the existing draft, targeting main; neither PR was merged by this task. Before the rewrite, the remote feature branch was verified to still point to `69afc0d4a796c3affcd8162be55447a567751031`. Publishing uses an explicit lease on that SHA, protecting any intervening remote work.

This refines the existing Phase 2A implementation. MUI remains the only component library. AuthProvider, JWT/session expiration, logout, Axios/ProblemDetail normalization, permission rules, routes, Vite proxy, backend and database schemas are unchanged. Customers remains a presentation template. No Phase 2B CRUD or business API integration was added.

## Audit findings and corrections

| Confirmed issue | Correction |
| --- | --- |
| Shell/login colors duplicated outside the theme; generic top-bar context | Typed `navigation` and `surface` palette tokens; current route title in the top bar. |
| Secondary labels missed 4.5:1 contrast on the light canvas; selected navigation badge had low contrast | Darkened light secondary text to `#5E6E83`; navigation badges use the brighter navigation text token. Input placeholders are opaque secondary text, and outlined field boundaries have stronger contrast. |
| Navigation lists contained anchors directly under `ul` | Proper `li` wrappers retain link semantics, grouping and `aria-current`. |
| Small icon targets and inconsistent keyboard indicators | 44px icon-button and button targets, larger checkbox hit areas, visible focus outlines, larger filter chips on coarse pointers. |
| Dialog reached through a row menu sometimes focused the container instead of Cancel in Chromium | Wait for the menu to exit before opening the dialog; explicitly focus Cancel after entry and restore the originating row button after exit. |
| Table had no reusable loading state; dialog had no pending state | Loading preserves column headings, announces progress and disables pagination. Pending dialogs block confirmation, cancellation, Escape and backdrop dismissal. |
| Status presentation depended on callers supplying arbitrary labels/colors | Typed labels and semantic colors for existing crane/job/maintenance/invoice statuses and active/inactive records; custom label/tone support retained. |
| Dashboard heading disappeared during loading/errors; repetitive helper text/icons | Stable page heading in every query state, contextual icons/supporting text and restrained success/warning accents. |
| Filters did not expose an active count | Visible and accessible active-filter count; status chip announces its pressed state. |
| Vitest dependency tree had security advisories | Targeted Vitest 4.1.12 upgrade with matching lockfile; see audit results below. |

## Design system

The supplied fleet/rental references inform hierarchy, grouping and table controls without copying their brands, assets or unsupported analytics. The light experience uses navy navigation (`#102D46`), slate text (`#17324D`), teal actions (`#087F78`), a cool canvas (`#F4F7FA`) and white bordered cards. Dark mode uses a `#101A26` canvas, `#0C1724` navigation, `#182635` surfaces and `#60CDBB` teal. Semantic success/warning/error colors have separate light/dark values.

`src/app/theme.ts` owns colors and component defaults. `palette.navigation` covers background, text, muted text, hover/selection, accent and border. `palette.surface` covers muted toolbar and accent icon surfaces. Standard MUI primary/text/background/divider/status tokens serve the remaining components. Inter is used when installed, with system fallbacks; no font asset or external font request was added.

The existing 8px spacing rhythm, 12px card corners, smaller control corners and restrained elevation are retained. Overrides cover buttons, icon buttons, checkboxes, chips, fields, tables, drawers, app bar and focus indicators. Motion is suppressed when reduced motion is requested, including drawer/menu/dialog transitions. KPI cards use aligned supporting text and responsive count sizing. There are no invented revenue, utilization, growth, ranking or historical comparison values.

The shell retains grouped, role-filtered navigation, a skip link, breadcrumbs, mobile drawer, user identity and theme toggle. Hidden modules remain protected on direct navigation. Theme preference and authentication remain memory-only as in Phase 1.

## Reusable component structure

| File | Components and contract |
| --- | --- |
| `src/shared/ui/Layout.tsx` | `PageHeader`: eyebrow, title, description, breadcrumb and action slots. `SectionCard`: section heading/body boundaries. `StatCard`: value, helper text, icon and semantic accent. |
| `src/shared/ui/Filters.tsx` | Controlled `SearchField`, responsive `TableToolbar`, wrapping `FilterToolbar` with active count, clear and more-filter actions. |
| `src/shared/ui/DataTableShell.tsx` | Generic `Column<T>` and `DataTableShell<T>` with stable row keys, scoped headings, labelled keyboard-scrollable region, loading/empty slots and controlled pagination. Also `StatusChip` and `ConfirmDialog`. |
| `src/shared/States.tsx` | Empty/loading states, forbidden state, and distinct session-ended, validation, conflict and unexpected-error headings. Safe messages still come from the existing API normalizer. |
| `src/app/AppShell.tsx` | Responsive navigation/frame, route context, identity, breadcrumbs and theme control. |

The table caller supplies visible rows, total count and pagination callbacks. It performs no fetching or server pagination itself. The loading state hides stale rows and the empty state, preserves headings and sets `aria-busy`. A test with separate pages verifies controlled pagination beyond the three-row demonstration.

StatusChip supports `AVAILABLE`, `IN_USE`, `MAINTENANCE`, `PENDING`, `COMPLETED`, `CANCELED`, `SCHEDULED`, `IN_PROGRESS`, `DRAFT`, `REVISED`, `ACTIVE` and `INACTIVE`. Every status has readable text. ConfirmDialog has unique title/description IDs, safe initial focus, focus trapping, optional explicit return focus, contextual labels and controlled `pending`/`pendingLabel` props. Its owner still supplies mutation/error/authorization behavior in future modules.

## Management page pattern

Customers starts with a meaningful empty state and disabled Add action. An explicit preview control reveals three local **Example company** rows with **EXAMPLE** references. The persistent notice states that records and actions are not connected. Sample records are not customer DTOs or PostgreSQL data.

The page composes PageHeader, a search toolbar, status filter, active-filter count/clear action, optional right-side filter drawer, table, status chips, row menu and pagination controls. Search/status filters operate only on opted-in fixtures and reset pagination. View/edit entries are disabled; the enabled menu item explains the example. No create/edit/delete forms or business requests exist.

For Phase 2B, keep this composition while replacing fixtures with a reviewed endpoint contract and TanStack Query. Keep UI state in the feature, map ProblemDetail validation errors, use dedicated lookup contracts and distinguish read access from write/transition permissions. Do not infer server pagination support from the reusable table API.

## Verification executed

From `frontend/`, using Node 24.19.0 and npm 11.9.0:

```sh
npm ci --cache /workspace/.npm
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
npm audit --cache /workspace/.npm
```

The cache argument selects a writable environment cache. Clean installation, TypeScript, ESLint, Prettier, tests and production build all passed. **54 tests passed in three files**: the existing 24 Phase 1 tests, eight Phase 2A interaction tests, and 22 refinement cases. Existing assertions were retained, with asynchronous expectations adjusted for the stable loading heading and menu exit transition. The test mount uses the actual application theme.

Coverage includes login/logout/expiration/401 handling, role-filtered navigation and forbidden direct URLs, API error sanitization, dashboard loading/data/empty/retry behavior, shell identity/breadcrumbs/current route, theme toggling without logout, mobile drawer selection/close, sample search/status/clear controls, drawer filtering, row menus, pagination, table loading, all twelve status labels, dialog focus restoration/trapping/pending behavior, breadcrumb composition and 401/403/409/500 presentations. MSW rejects unhandled requests; template tests add no customer API handler.

Installation still reports existing `eslint@9.39.5` and `whatwg-encoding@3.1.1` deprecation notices. Production build reports upstream Zod comment-annotation warnings; Rollup removes those comments and completes successfully. These warnings were not suppressed, and unrelated tooling was not upgraded.

### Dependency security

The initial audit reported **three findings: one moderate and two critical**, involving Vitest, `@vitest/mocker` and `tinypool`. Vitest was upgraded from 3.2.7 to exact **4.1.12**, the minimum inspected compatible release covering the mocker code-injection advisory fixed in 4.1.12 (4.1.11 only addressed the earlier path-traversal issue). Its dependency tree also removes the vulnerable tinypool dependency. Node/Vite compatibility and all existing tests were verified. No `npm audit fix --force` or unrelated direct upgrade was used.

The final `npm audit` reports **zero vulnerabilities**. `package.json` and `package-lock.json` are committed together. Browser audit tooling was installed temporarily outside the repository; no runtime or browser-test dependency was added to the frontend.

### Browser and accessibility review

Real Chromium checks use temporary Playwright and axe-core tooling with intercepted login/current-user/dashboard fixtures, **not a live backend**. Only those three API routes are allowed. The matrix covers 390px, 768px, 1366px and 1440px widths in both themes: login, dashboard, empty/sample Customers, filters, menus, dialog and pagination, plus navigation drawers below 900px. Mobile/tablet contexts emulate touch input and reduced motion. Keyboard checks exercise filter/menu/dialog dismissal, dialog trapping and focus return to the row/drawer triggers.

The final matrix completed **60 scans with zero reported axe violations**, zero page errors and no page-level horizontal overflow. Tables scroll within their own region. Automated checks also confirmed sample filtering, bounded pagination, dialog focus trapping and focus restoration. Desktop/mobile/tablet screenshots were inspected for clipping, hierarchy, contrast and spacing. Screenshots were captured separately from axe. The drawer image captures the visible viewport so the fixed overlay is shown in its actual on-screen position.

Axe still marked items for manual review: contrast, focusability under modal-hidden roots, and column headings in intentionally empty tables. Visual token/layout review and keyboard checks supplement those results; they do not establish that every WCAG criterion passes. Empty tables deliberately retain their headings. Assistive-technology verification remains outstanding.

The audit checks WCAG 2.0/2.1/2.2 A/AA rules supported by axe and includes visual inspection of the captured layouts. This is not WCAG certification. Screen-reader announcements, Safari/Firefox, real devices, zoom/reflow beyond the listed viewport matrix, and live backend integration remain unverified.

### Review screenshots

Identity and dashboard values in every image are test fixtures, not seeded records.

- [Dashboard — light](images/phase-2a/dashboard-light.png)
- [Dashboard — dark](images/phase-2a/dashboard-dark.png)
- [Dashboard — mobile](images/phase-2a/dashboard-mobile.png)
- [Dashboard — tablet](images/phase-2a/dashboard-tablet.png)
- [Customers — light](images/phase-2a/customers-light.png)
- [Customers — dark](images/phase-2a/customers-dark.png)
- [Customers — mobile](images/phase-2a/customers-mobile.png)
- [Filter drawer — mobile](images/phase-2a/filters-mobile.png)

## Files changed by this refinement

- `frontend/src/app/theme.ts`, `AppShell.tsx`: semantic tokens, contrast/focus/targets, list semantics and route context.
- `frontend/src/features/auth/LoginPage.tsx`: theme tokens only; form/auth behavior retained.
- `frontend/src/features/dashboard/DashboardPage.tsx`: persistent heading, KPI descriptions/icons/accents.
- `frontend/src/features/management/ManagementTemplate.tsx`: filter indication, accessible row menus/drawer and focus handoff.
- `frontend/src/shared/ui/{Layout,Filters,DataTableShell}.tsx` and `frontend/src/shared/States.tsx`: reusable behavior described above.
- `frontend/src/test/{app,ui-polish,refinement}.test.tsx`: retained/updated tests and refinement regressions.
- `frontend/package.json`, `frontend/package-lock.json`: targeted test-tool security upgrade.
- This report and eight screenshots under `docs/images/phase-2a/`.

## Limitations and Phase 2B readiness

The foundation is ready for Phase 2B planning and review after approval of this draft PR. Customer directory, crane inventory and quotations are sensible next modules, each with verified API/lookup contracts, separate read/write permissions, validation and recovery tests. Scheduling/job workflows should follow those foundations.

Customers is deliberately a layout reference. Sorting, selection, export, real pagination and mutations require separate endpoint/UX decisions. Dialog pending state is presentation behavior; it does not authorize or implement a mutation. Theme/session persistence is unchanged. Live backend integration and the accessibility/browser checks listed above remain rollout work. No Phase 2B implementation was started.
