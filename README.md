# Clearwave Kensa Example

A showcase project demonstrating [Kensa](https://kensa.dev) — BDD testing for Kotlin & Java.

The domain is a fictional telecoms provider ("Clearwave") with two services under test:

- **FeasibilityService** — checks whether a broadband service can be delivered to a given address
- **OrderService** — places a broadband order and coordinates with external network and tracking systems

Tests are written using the Kensa Given-When-Then DSL with [http4k](https://http4k.org) stubs standing in for downstream APIs. The HTML report generated from these tests is published as a live example at:

**[kensa-dev.github.io/clearwave-kensa-example](https://kensa-dev.github.io/clearwave-kensa-example)**

## Running locally

```bash
./gradlew test
```

The report is written to `build/kensa-output`. Open `index.html` in a browser, or serve it with the Kensa CLI:

```bash
kensa --dir build/kensa-output
```

## Purpose

This project serves two roles:

1. **Showcase** — a realistic example of Kensa tests that visitors to [kensa.dev](https://kensa.dev) can explore. The tests and domain are updated alongside the documentation.

2. **Canary** — run as part of Kensa's CI on every commit to master, building against the latest snapshot to catch regressions early.

## Dependencies

| Library | Role |
|---|---|
| [Kensa](https://kensa.dev) | BDD test framework |
| [http4k](https://http4k.org) | HTTP client & stub server |
| [Kotest](https://kotest.io) | Assertions |
| [JUnit 5](https://junit.org/junit5/) | Test runner |
