# Starbank

Integration with Stark Bank (Sandbox): periodically issues Invoices and, upon
receiving the payment webhook, sends a Transfer with the net amount.

Built for the Stark Bank Back End Developer Trials.

## Stack

- Kotlin + Spring Boot 4.1 / Java 21
- H2 (in-memory) + Flyway
- Micrometer + Prometheus

## Status

Work in progress. See `docs/adr/` for architecture decisions.

## Running locally

```bash
./gradlew bootRun
```

## Structure

```
src/main/kotlin/com/starkbank/challenge/
├── domain/              # Invoice, Transfer, WebhookEvent
├── infra/persistence/   # Repositories
└── ...
```
