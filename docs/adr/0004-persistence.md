# ADR-0004: Postgres (Cloud SQL) in the cloud, H2 for local dev and tests

## Status

Accepted.

## Context

State (invoices, transfers, webhook events, and the 24h run-start timestamp
from [ADR-0003](0003-scheduler.md)) must survive Cloud Run scaling an
instance to zero, which happens by design between requests. Losing the
run-start timestamp would restart the 24h window on every cold start;
losing invoice records would break the authenticity check in
[ADR-0002](0002-webhook-security.md).

## Decision

- Local dev and tests use H2 in-memory, selected via the `local` Spring
  profile, no infrastructure required to run `./gradlew test`.
- The cloud deployment uses Cloud SQL for Postgres, selected via a `cloud`
  profile, connected through the Cloud SQL Auth Proxy sidecar (or the
  built-in Cloud Run to Cloud SQL Unix socket connector), not a public IP.
- Flyway migrations are written in plain SQL (see `db/migration/V1__initial_schema.sql`)
  specifically so the same migration runs unmodified against both H2 and
  Postgres. No database-specific SQL features are used.

## Consequences

- Two datasource configs to maintain (`application-local.yml`,
  `application-cloud.yml`), but the schema and application code stay
  identical across both.
- Cloud SQL has its own free-tier limits and a cold-start cost on the
  connection; acceptable for this challenge's traffic (dozens of requests
  over 24h).
- The Cloud SQL instance and credentials become part of the deployment
  surface: connection name, database user, and password need to be
  provisioned and passed to Cloud Run as configuration (see [ADR-0005](0005-cloud-deployment.md)
  for how secrets are handled).
