# ADR-0005: Deploy to Google Cloud Run

## Status

Proposal.

## Context

The challenge explicitly calls out cloud deployment as a bonus point, with
free tiers on Google Cloud, AWS, or Azure suggested as options. The goal is
a deployment that is not just "up", but demonstrates sound cloud
architecture: no long-lived idle compute cost, no plaintext secrets, and a
deploy path that is reproducible, not a set of manual console clicks.

## Decision

Deploy to Google Cloud Run, with:

- **Container image**: built via the Spring Boot Gradle plugin's native OCI
  image support (`./gradlew bootBuildImage`), no hand-written Dockerfile
  needed, pushed to Artifact Registry.
- **Compute**: Cloud Run service, scale-to-zero enabled (default). This is
  why the scheduler had to move out of the process, see [ADR-0003](0003-scheduler.md).
- **Database**: Cloud SQL for Postgres, see [ADR-0004](0004-persistence.md).
- **Secrets**: the Stark Bank private key and the Cloud SQL password are
  stored in Secret Manager and mounted into Cloud Run as environment
  variables at deploy time, never committed to the repo or baked into the
  image. This directly satisfies the Stark Bank SDK's own warning ("never
  hardcode private keys in source, store them in an HSM or at minimum an
  encrypted KMS").
- **Two HTTP surfaces with different exposure**:
  - `POST /webhook`: public, since Stark Bank's servers call it directly.
    Authenticity is guaranteed by signature verification ([ADR-0002](0002-webhook-security.md)),
    not by network restriction.
  - `POST /internal/emit-invoices`: private, restricted at the IAM layer to
    the Cloud Scheduler job's service account only ([ADR-0003](0003-scheduler.md)).
- **Deploy path**: a GitHub Actions workflow builds the image, pushes to
  Artifact Registry, and deploys to Cloud Run on push to `main`, so the
  deployment is reproducible from the repository, not a manual step.

## Consequences

- Free-tier friendly: Cloud Run bills per request/CPU-time, not per idle
  hour, and the challenge's total traffic (dozens of invoices and webhook
  calls over 24h) comfortably fits the always-free tier.
- Adds real infrastructure to provision and document: Artifact Registry,
  Cloud SQL instance, Secret Manager entries, the Cloud Scheduler job and
  its service account, and IAM bindings. This is worth documenting in the
  README as a "how to reproduce this deployment" section, since a reviewer
  should be able to follow it without guessing.
- Cold starts are possible between the 3h-spaced invocations; acceptable
  for this workload, would need `min-instances` tuning for a latency
  sensitive production service, not needed here.
