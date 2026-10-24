# React Phase 1 — Foundation and authentication

Branch: `feat/react-foundation-auth`, based on main `bff3254` (merged Phase 0B-2).

## Source-of-truth inspection

Implementation follows AuthController, SecurityConfig, JwtAuthenticationFilter, UserRole, DashboardController/DashboardResponse, controller method authorization annotations, and the Phase 0A and Phase 0B-2 contracts. No backend files changed.

- POST `/api/auth/login` takes `{email,password}` and returns **raw JWT text**, not a JSON token object.
- GET `/api/auth/me` returns `{id,username,email,role}`. Zod validates this identity before opening protected pages. Roles come from this response, never decoded JWT claims.
- GET `/api/dashboard` is available to all authenticated users. Cards display exactly `totalCranes`, `availableCranes`, `inUseCranes`, `maintenanceCranes`, `pendingJobs`, `completedJobs`, `canceledJobs`, `totalInvoices`, `draftInvoices`, `completedInvoices`, `revisedInvoices`, `canceledInvoices`. Missing values display an em dash rather than invented zeroes. No revenue data is inferred.

## Architecture and dependencies

`frontend/` is an independent Vite project using strict TypeScript. React 19, MUI 7 plus Emotion and MUI Icons provide a coherent responsive design system. React Router 7 owns navigation, Axios 1 handles transport, TanStack Query 5 owns server state, and React Hook Form 7 with Zod 4/resolvers 5 handles validation. No Redux or second component library. Exact installed versions are reproducible through `package-lock.json`; `npm ci` is the installation path.

Vitest 3, React Testing Library, user-event, jsdom and MSW 2 test observable behavior with HTTP mocks. TypeScript 5, ESLint with TypeScript/React Hooks rules, and Prettier provide static and formatting checks. Compatible stable major versions are selected explicitly; resolver 6's optional-peer conflict was avoided without force/legacy-peer-deps. ESLint 9 is compatible but npm marks this major unsupported; upgrade the lint toolchain together in a follow-up.

```text
frontend/
  src/main.tsx                  bootstrap
  src/app/App.tsx              providers, theme, routing and responsive shell
  src/features/auth/          memory session, identity validation, login form
  src/features/dashboard/     count cards and query states
  src/shared/api.ts           single Axios client and error normalization
  src/shared/permissions.ts   full-record read permissions and navigation
  src/shared/States.tsx       reusable loading/error/access-denied states
  src/test/                   MSW lifecycle and application regression tests
```

## Local environment and startup

Requires Node 22.12+ (verified here with Node 24) and npm. Start the backend using its existing environment configuration, then:

```sh
cd frontend
npm ci
npm run dev
```

Open the Vite URL printed in the terminal (normally http://localhost:5173). Vite proxies `/api` to `http://localhost:8080`. Override the server-side target with `BACKEND_URL=http://localhost:8081 npm run dev`. This variable is read by Vite's server configuration; no backend secrets or credentials are bundled into browser code. There are no frontend secrets or JWT environment variables.

For production, serve `frontend/dist` behind the same origin as a reverse proxy for `/api` to Spring Boot. Configure SPA history fallback to `index.html` for frontend paths, **excluding `/api`**. The Vite development proxy does not exist in the production bundle. Separate-origin deployment/CORS is not configured in this phase.

## Authentication and authorization

Login requests explicitly use Axios text response parsing. The token is stored only in a module's memory, attached as Bearer authorization to subsequent requests, and discarded on logout, 401 or expiry. Neither localStorage nor sessionStorage stores authentication. Reload requires login. JWT expiry is decoded only to schedule local session termination; signature/account validity remains the backend's responsibility. No tokens are printed or included in user-facing exception messages.

The provider fetches `/auth/me` before admitting the user. An attempt generation prevents late identity responses from restoring a cleared session. Timers/subscriptions are cleaned up on unmount. Logout and authenticated 401 clear TanStack Query's cache. Late 401s carrying a different old token do not terminate the current session. Login failures are displayed once at the form; 403 preserves authentication and displays access denied.

All application routes require authentication. `Protected` additionally accepts a permission key; `can` is reusable. Centralized **read/navigation** permissions mirror the full-record controllers (lookup access does not grant full-record access):

| Area | Roles |
| --- | --- |
| Overview | All five roles |
| Cranes, quotations | ADMIN, SALES |
| Customers, job cards | ADMIN, SALES, OPERATIONS |
| Timesheets | ADMIN, OPERATIONS |
| Maintenance | ADMIN, OPERATIONS, MAINTENANCE |
| Invoices | ADMIN, FINANCE |
| Users | ADMIN |

The sidebar filters these entries. Future modules are explicitly marked **Planned** and open an informational shell only, protected by the corresponding read permission. No CRUD screens, record queries, reports or administrative actions are implemented for these modules. Phase 2 must add separate write/transition permissions based on their method annotations; navigation read access must never imply write access. Backend authorization remains authoritative.

## UI and error behavior

Responsive desktop sidebar/mobile drawer, application bar, current identity, logout, breadcrumbs, light/dark theme toggle, restrained teal operations palette, count cards, and loading/empty/error/not-found/access-denied states are included. Theme preference is memory-only. No external fonts, imagery, animation framework or mock business statistics are used.

One Axios client normalizes ProblemDetail, including JSON sent as text on login errors. `ApiError` retains HTTP status, stable code, safe message and field-level errors. HTTP 400 fields map onto known form fields; otherwise one summary is shown. HTTP 409 uses the business detail. HTTP 401 clears an authenticated session and routes back to login; HTTP 403 never logs out. HTTP 500 and unexpected exceptions display generic text, suppressing technical details. Dashboard retries are user-driven; automatic retries are disabled to avoid repeated errors. Query functions accept cancellation signals.

## Verification

```sh
cd frontend
npm ci
npm run typecheck
npm run lint
npm test
npm run format:check
npm run build
```

Tests cover raw-token parsing and authenticated identity headers, identity loading/failure, login success/failure, client/backend validation, logout, protected routes, timed expiry, authenticated 401/cache clearing, 403 retention, all five role navigation matrices, direct forbidden-route denial, dashboard loading/success/missing/empty/failure/retry, and ProblemDetail normalization. Results are recorded after execution below.

No backend code was modified, so Maven verification is not rerun for this frontend-only phase. This phase's API tests use MSW; they do not claim a real-browser, real-backend login or visual cross-browser test.

## Limitations and Phase 2

- Refresh and new tabs require login; there is no refresh-token endpoint or silent session restoration.
- Frontend permissions and displayed identity update at login. Backend role/deactivation checks apply on every request; 403 is shown without discarding the session, while deactivation yields 401.
- Only dashboard counts are integrated. Planned routes intentionally provide no business workflows.
- Production routing/reverse proxy must be configured by deployment infrastructure.
- Recommended next work: approved business-module read/create/edit workflows, explicit mutation permission maps, lookup integrations, scheduling/quotation validation UX and 409 recovery, with endpoint-specific tests. Add real-browser accessibility/responsive and end-to-end backend checks before production rollout. Reporting charts and any auth-persistence redesign require separate scope.

## Executed results

Dependency installation and lockfile reinstall (`npm ci --ignore-scripts`, using a writable cloud npm cache) succeeded. Final `npm run typecheck`, `npm run lint` (zero warnings/errors), `npm test` (**24 passing tests**), `npm run format:check`, and `npm run build` all succeeded. The production build splits React, MUI/Emotion and application code into approximately 222/243/260 kB chunks (69/75/85 kB gzip), avoiding the initial single-chunk size warning. Rollup reports harmless upstream Zod comment-annotation warnings; it removes those comments and builds successfully. npm also reports the documented ESLint-major and jsdom transitive encoding-package deprecations.

## Files changed

All implementation files are new under `frontend/`: package manifest/lockfile, HTML/bootstrap, strict TypeScript and Vite/Vitest configuration, ESLint/Prettier configuration, app/providers/router/shell, auth provider/login, dashboard, shared API/permissions/UI states, and MSW/application tests. `docs/frontend-phase-1.md` contains this implementation and verification record. Backend, schema and existing Phase 0A/0B code are unchanged.
