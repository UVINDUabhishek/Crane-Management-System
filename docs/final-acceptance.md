# CHMS final acceptance — October 29, 2026

## Delivery and scope

Working branch: `feat/react-phase-2b2-crane-management`. Application acceptance tested implementation `992567c3aadf692cdcb329f440df0d9e0c2d5b04`; the final delivery commit additionally corrects one test typing issue and adds this acceptance record. The bundle/manifest identifies the final SHA. No push or merge was performed.

Implemented routes: `/login`, `/` (dashboard), `/workspace/customers`, `/workspace/cranes`, `/workspace/quotations`, `/workspace/jobs`, `/workspace/timesheets`, `/workspace/maintenance`, `/workspace/invoices`, `/workspace/users` (including operators and admin password changes), `/workspace/reports`. Navigation and action permissions use the existing backend rules. No requested module remains a Planned placeholder.

The remote feature branch was verified at `0688484171d9f2ddc79513f87d47693ea0feb9f3`; implementation `992567c3` and this acceptance commit are local delivery work. The Windows directory is not mounted in this environment, so its synchronization cannot be asserted. The existing Windows project was not changed.

## Real-browser acceptance

Actual system: PostgreSQL 17 in a new disposable Docker container, the built Java 21/Spring Boot application on port 18080 with real datasource/JWT environment variables, Vite on port 5173 with its actual proxy, system Chromium driven by Playwright. No API intercepts, MSW handlers or mocked browser responses were used. The temporary admin was bootstrapped only in this isolated database; four other role accounts were created through the UI.

**Passed:** login and real dashboard; customer/crane/quotation/job creation; timesheet entry; dedicated job completion; exactly one automatically generated invoice (8 billable hours × 1000 + 500 mobilization = 8500); invoice completion; report display of real revenue 8500; maintenance creation/start/completion; crane deactivation blocked by both pending jobs and scheduled maintenance with persisted Active state; successful deactivation/reactivation after work finished; admin user creation; SALES/OPERATIONS/FINANCE/MAINTENANCE logins, navigation restrictions, direct admin-route denial and actual admin API 403; logout.

19 recorded acceptance checks passed. Zero JavaScript page exceptions or failed network transports. Two expected API 409 responses exercised crane protections. Additional authenticated API probes returned the expected four 403 responses. The browser console also reported two non-blocking `/favicon.ico` 404 messages; no favicon is shipped. No business-functionality blocker was found. Initial harness attempts needed label matching/background-dialog selector corrections; those were automation issues, not application fixes. The successful complete run is recorded in [live-browser-results.json](acceptance/live-browser-results.json).

Desktop Chromium/Linux acceptance is verified. Native Windows, mobile-device/browser combinations, screen-reader acceptance, exhaustive CRUD permutations and production deployment/security auditing are not claimed.

## Final automated verification

| Check | Result |
| --- | --- |
| TypeScript | Passed |
| ESLint | Passed |
| Prettier format:check | Passed |
| Vitest | 144 passed; 0 failed, 0 skipped; 6 files; 65.51 seconds |
| Vite production build | Passed |
| Maven clean verify | BUILD SUCCESS; 553 tests, 0 failures/errors/skips |
| JaCoCo | All existing thresholds passed; 46 classes analyzed |

Commands executed: frontend `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run test`, `npm run build`; backend `JAVA_HOME=/workspace/jdk21 MAVEN_USER_HOME=/workspace/.m2 ./mvnw -Dmaven.repo.local=/workspace/.m2/repository clean verify`, with generated datasource/JWT environment values. The temporary local PostgreSQL instance supplied application-context prerequisites; integration tests retained PostgreSQL Testcontainers. The frontend checks ran after the test typing fix; the test file was formatted before the final formatting/test/build sequence.

A final typecheck exposed an invalid `exact` option in the newly strengthened React Testing Library report assertion. Removing that unsupported option retains exact string-name matching and the revenue-data checks. No production source or business rule changed during acceptance. Existing non-fatal Zod annotation build warnings remain visible. No tests, coverage gates or security checks were disabled.

## Configuration and Windows 11 startup

Requirements: Git, a full JDK 21 (JAVA_HOME configured), Node compatible with Vite 7 (Node 24 was tested), PostgreSQL, and Docker Desktop for Testcontainers verification. Runtime requires a non-production database, `DB_PASSWORD`, and a strong `JWT_SECRET`. Optional overrides: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`; frontend `BACKEND_URL` defaults to `http://localhost:8080`. Never commit local credentials. Hibernate uses the existing `ddl-auto=update`; start against a dedicated review database, not an unknown production database.

1. In PostgreSQL/IntelliJ Database tools, create a dedicated database once: `CREATE DATABASE chms_review;`. Open the delivered root Maven project in IntelliJ, set Project SDK and Maven Runner to JDK 21. In a PowerShell terminal at the repository root:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/chms_review'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$securePassword = Read-Host 'Local PostgreSQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [BitConverter]::ToString($bytes).Replace('-', '')
.\mvnw.cmd spring-boot:run
```

Equivalent IntelliJ Run Configuration: `CraneManagementSystemApplication`, with the same environment variables. The backend listens on 8080. The generated JWT secret must be consistent for an application's running instances; changing it invalidates earlier tokens. No permanent secret is supplied in this document.

2. Reuse an administrator in an existing **development** database if available. For a completely new review database there is no public admin registration endpoint. After the application creates its tables, run this once in that review database's SQL console, substituting a unique local password without committing it:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO users (username, email, password, role, active, created_at, updated_at)
VALUES ('Review administrator', 'review-admin@example.test',
        crypt('<choose a unique development password>', gen_salt('bf')),
        'ADMIN', true, now(), now())
ON CONFLICT (email) DO NOTHING;
```

This requires appropriate local database privileges and never overwrites an existing account. Sign in with the chosen credentials, then create dedicated SALES, OPERATIONS, FINANCE and MAINTENANCE users through Users. No fixed/shared demo password or test-runtime secret is included in delivery. The acceptance accounts and data were disposable and are not a required application dependency.

3. In a separate VS Code/PowerShell terminal:

```powershell
Set-Location 'C:\Users\UVINDU\Desktop\Projects\chms-review\frontend'
npm.cmd ci
npm.cmd run dev
```

Open `http://localhost:5173` in Chrome. If backend port differs, set `$env:BACKEND_URL = 'http://localhost:<port>'` before starting Vite. Frontend API calls stay under `/api` and use the proxy. Existing sessions are memory-only: refreshing the browser requires sign-in again.

4. Engineer verification from frontend: `npm.cmd run test:doctor`, `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run format:check`, `npm.cmd run test`, `npm.cmd run build`. From root, with Docker Desktop running and the same database/JWT environment configured: `.\mvnw.cmd clean verify`. The application-context test uses the configured database; integration tests use isolated PostgreSQL Testcontainers. Use a review database throughout.

## Safe offline delivery and repository hygiene

A complete Git bundle is supplied separately with a SHA-256 checksum manifest. It includes branch history and all required tracked source/configuration/tests/docs. A new review clone is the safest option because it leaves `crane-management-system-final` and other old project directories intact:

```powershell
git clone --branch feat/react-phase-2b2-crane-management 'C:\Downloads\CHMS-final.bundle' 'C:\Users\UVINDU\Desktop\Projects\chms-review'
Set-Location 'C:\Users\UVINDU\Desktop\Projects\chms-review'
git bundle verify 'C:\Downloads\CHMS-final.bundle'
git rev-parse HEAD
git status --short
git remote set-url origin 'https://github.com/UVINDUabhishek/Crane-Management-System.git'
```

Compare HEAD with the delivery manifest. If importing into an existing clone instead, preserve/review dirty work first, fetch the bundle into a separate delivery tracking ref, inspect the diff and use only a fast-forward when histories allow it. Do not reset/clean/force-push to make it fit.

Tracked-file credential-pattern review found no literal database/JWT credentials, private keys, AWS access keys or GitHub tokens. Application config uses environment placeholders; existing test credentials are test fixtures. This is a focused hygiene check, not a formal secrets/security audit. No required source/config files were untracked before acceptance; temporary runtime credentials, logs, browser tooling, databases and generated artifacts are outside the delivery source tree. `npm ci` and Maven resolve dependencies from the committed manifests.

Remaining limitations: native Windows execution unverified here; harmless favicon 404; full-array client-side pagination; report relationship filters use numeric IDs; quotation/user responses lack activation flags; first admin requires development bootstrap or an existing account. None blocked the tested hire transaction. Reviewer acceptance and deployment decisions remain pending; PR #7 stays unmerged.

The isolated acceptance runtime was stopped after verification; its disposable accounts are not delivered. Start the review system using the configuration above.
