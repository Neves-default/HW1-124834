# TuttiBox — HW1 (TQS 2026/27)

Smart parcel lockers: partners register parcels for a locker station, a courier deposits them
at the locker terminal, and the recipient collects them with a single-use pickup code.
An operator supervises each station.

**Stack:** Spring Boot 4 (Java 21, Maven) · Thymeleaf · Spring Data JPA · PostgreSQL (Docker) ·
OSM Nominatim (geocoding). Tests: JUnit 6, AssertJ, Mockito, Testcontainers, WireMock, Cucumber,
Playwright, k6, JaCoCo.

## Assumptions

- One city (Aveiro). Stations and compartments are seed data (`DataSeeder`), loaded on the first start.
- No authentication: each actor has its own pages/endpoints.
- The locker hardware is an in-memory fake (`FakeLockerHardware`). In the application it behaves
  like a working locker (demo mode); tests configure it to fail.
- Notifications (pickup code to the recipient, alerts to the operator) are written to the log
  channel `tuttibox.notifications`, which plays the role of the SMS gateway.
- Partners use the REST API only (no web page needed).

## Business rules (configurable in `application.yml`, prefix `tuttibox`)

| Rule | Value | Property |
|---|---|---|
| Compartment allocation | same size, otherwise next larger (S→M→L), never smaller | `allocation.allow-larger-size` |
| Drop-off deadline | 5 days after registration → CANCELLED | `deadlines.drop-off` |
| Pickup deadline | 5 days after drop-off → EXPIRED | `deadlines.pickup` |
| Pickup code | 6 random digits, single use, stored as HMAC, 5 wrong codes block the terminal for 2 min | `pickup-code.*` |
| Nearest stations | 5 results | `search.max-results` |
| Geocoding | Nominatim, 5 s timeout, ≥1 s between calls, cached, identifying User-Agent | `geocoding.*` |

Before running against the real Nominatim, put your contact e-mail in `tuttibox.geocoding.user-agent`.

## Build and run

Requirements: Java 21, Maven 3.9+, Docker.

```bash
docker compose up -d                 # PostgreSQL on localhost:5432
mvn spring-boot:run                  # http://localhost:8080
```

Pages: `/` (stations, links to each terminal and operator page) · `/search` · `/track` ·
`/terminal/{stationId}` · `/operator/{stationId}`.

To run without calling Nominatim (offline demo / load tests): `mvn spring-boot:run -Dspring-boot.run.profiles=loadtest`.

### REST API (summary)

| Method & path | Use case | Success | Errors |
|---|---|---|---|
| `GET /api/stations/nearest?address=` | UC1 | 200 | 400, 404 `ADDRESS_NOT_FOUND`, 503 `GEOCODING_UNAVAILABLE` |
| `POST /api/parcels` `{stationId,size,recipientContact}` | UC2 | 201 | 400, 409 `NO_FREE_COMPARTMENT` |
| `POST /api/parcels/{trackingCode}/cancel` | UC2 | 200 | 404, 409 `INVALID_TRANSITION` |
| `POST /api/terminal/{stationId}/drop-off` `{trackingCode}` | UC3 | 200 | 404, 409, 503 `HARDWARE_FAILURE` |
| `POST /api/terminal/{stationId}/pickup` `{pickupCode}` | UC4 | 200 | 400 `INVALID_CODE`, 429 `TERMINAL_BLOCKED`, 503 |
| `GET /api/parcels/{trackingCode}` | UC5 | 200 | 404 |
| `GET /api/operator/stations/{id}/parcels?state=` | UC6 | 200 | |
| `POST /api/operator/parcels/{trackingCode}/removal` | UC6 | 200 | 409 `INVALID_TRANSITION` |
| `POST /api/compartments/{id}/out-of-service` · `/reactivation` | UC6 | 200 | 409 |

Every error has the same body: `{"status": 409, "error": "NO_FREE_COMPARTMENT", "message": "..."}`.

## Tests

| Command | Runs | Docker? |
|---|---|---|
| `mvn test` | unit tests (`*Test`) | no |
| `mvn verify` | unit + integration + BDD (`*IT`, incl. `CucumberIT`) + JaCoCo report | **yes** |
| `mvn verify -Pe2e` | the above + Playwright browser tests (`*E2E`) | **yes** |
| `k6 run load-tests/search-and-register.js` | load test (app running with profile `loadtest`) | no |

- Coverage report: `target/site/jacoco/index.html`. Cucumber report: `target/cucumber-report.html`.
- The first Playwright run downloads the browsers automatically.
- No test calls the real Nominatim API.

| Level | Where | What |
|---|---|---|
| Unit | `domain/*Test`, `service/*Test` | transition table (parameterized, all state × event pairs), size compatibility, distance + ranking (Mockito), pickup code format/hash, terminal lockout |
| Integration | `it/ReservationServiceIT` | business rules through the services, real PostgreSQL (Testcontainers), fake hardware with injected failures, controllable clock |
| Integration | `it/AllocationConcurrencyIT` | parallel requests for the last free compartment |
| Integration | `it/ApiIT` | REST status codes and error body (MockMvc, full context) |
| Integration | `it/NominatimGeocodingClientIT` | geocoding client vs WireMock: found / empty / slow / 5xx / 429 / cache / User-Agent / rate limit |
| BDD | `src/test/resources/features`, `bdd/LockerSteps` | Gherkin scenarios for UC2, UC3, UC4, UC6, incl. time-based and hardware-failure scenarios (steps drive the service layer) |
| Functional | `e2e/*E2E` | Playwright: station search page and the terminal journey (drop-off + pickup) |
| Load | `load-tests/` | k6: 50 VUs, p95 < 1 s, errors < 1 % |

Test doubles: `FakeLockerHardware` (fake, hardware port), `RecordingNotifier` (fake, notification port),
`FakeGeocoder` (fake, geocoding port), `MutableClock` (controllable time), WireMock (stub HTTP server),
Mockito mocks in `StationServiceTest`.
# HW1-124834
