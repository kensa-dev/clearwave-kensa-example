# Clearwave Kensa Example

A showcase project demonstrating [Kensa](https://kensa.dev) — BDD testing for Kotlin & Java.

The domain is a fictional telecoms provider ("Clearwave") with two services and a small UI under test:

- **FeasibilityService** — checks whether a broadband service can be delivered to a given address
- **OrderService** — places a broadband order and coordinates with external network and tracking systems
- **Feasibility checker UI** — a Vite + React + shadcn page that submits the feasibility form to the live `FeasibilityService`, exercised end-to-end through both Playwright and Selenium

Tests are written using the Kensa Given-When-Then DSL with [http4k](https://http4k.org) stubs standing in for downstream APIs. The HTML report generated from these tests is published as a live example at:

**[kensa-dev.github.io/clearwave-example](https://kensa-dev.github.io/clearwave-example)**

## Running locally

The project has two test cycles.

### Service-level tests (canary build)

```bash
./gradlew test
```

Runs the http4k-driven `FeasibilityServiceTest` and `OrderServiceTest`. No browser needed. The report is written to `build/kensa-output`.

### UI tests

```bash
./gradlew installPlaywrightBrowsers   # one-off — installs Chromium for Playwright
./gradlew uiTest                      # builds the UI and runs Playwright + Selenium tests
```

UI tests need Chrome installed (Selenium uses it via Selenium Manager). The Vite UI is built automatically before the tests run. The report is written to `build/kensa-output-ui`.

To open either report:

```bash
kensa --dir build/kensa-output
kensa --dir build/kensa-output-ui
```

## Purpose

This project serves two roles:

1. **Showcase** — a realistic example of Kensa tests that visitors to [kensa.dev](https://kensa.dev) can explore. The tests and domain are updated alongside the documentation.

2. **Canary** — run as part of Kensa's CI on every commit to master, building against the latest snapshot to catch regressions early. (UI tests are excluded from the canary because they need browsers.)

## Dependencies

| Library | Role |
|---|---|
| [Kensa](https://kensa.dev) | BDD test framework |
| [Kensa Playwright](https://kensa.dev) / [Kensa Selenium](https://kensa.dev) | UI testing drivers |
| [http4k](https://http4k.org) | HTTP client & stub server |
| [Vite](https://vite.dev) + [React](https://react.dev) + [shadcn/ui](https://ui.shadcn.com) | Feasibility UI |
| [Kotest](https://kotest.io) | Assertions |
| [JUnit 5](https://junit.org/junit5/) | Test runner |
