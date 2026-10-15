# SAFARI Runtime Validation Report

**Generated:** 2026-10-07  
**Target:** `c:\Users\TERA\Desktop\almacen-java-prueba`

## Summary

| Step | Status | Exit code | Details |
|---|---|---:|---|
| Startup | PASS | n/a (stopped intentionally) | SAFARI started with in-memory H2; `/login` loaded in 7.7 seconds. The Maven run was then stopped during cleanup. |
| Integration tests | PASS | 0 | 22 Maven tests passed; 0 failures, 0 errors, 0 skipped. |
| Browser checks | PASS | n/a | Admin login, product create/edit/delete, a sale, stock decrement, sales history, and protected admin pages were exercised. |

**Overall:** PASS, with the production MySQL connection not exercised.

## Environment

- Java: 17.0.19
- Docker: unavailable (Docker CLI not present); integration tests used H2 in memory.
- Node.js: 24.18.0 available only as a possible browser-test tool; it is not part of SAFARI or its build.
- Browser: the integrated browser was used. No local Playwright browser cache was present, and no browser was downloaded.
- The temporary runtime used a separate H2 database and was stopped after validation; it did not connect to or alter the configured MySQL database.

## Integration Test Coverage

The canonical command is:

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd test
```

The three added integration suites cover:

- Admin product, category, brand, supplier, user, and role create/read/update/delete operations.
- Admin and non-admin access checks, SAFARI demo login, password hashing, and protected screens.
- Receiving stock, recording sales, verifying movement headers/details, calculating totals, checking stock changes, and preventing overselling.

All tests use the project's H2 test configuration. The existing role-access, validation, movement-service, and web-controller suites also passed.

## Browser Evidence

- Login page title: `Login - SAFARI`; the seeded admin account opened the admin catalog.
- Product create and edit succeeded; the changed name appeared in the list.
- A sale appeared in the sales history for `12.50`, and the product stock went from 2 to 1.
- A separate product without movements was deleted successfully.
- `/catalogos`, `/usuarios`, `/roles`, `/entradas`, and `/salidas` each returned HTTP 200 for the admin session.

## Known Gaps

- No live MySQL/XAMPP database was available for a database-engine-specific validation. H2 compatibility does not prove MySQL connectivity or schema parity; verify those against the intended `safari` database before using real inventory data.
- Browser flow was exercised in the integrated browser; no standalone Playwright test dependency or JavaScript application code was added.
