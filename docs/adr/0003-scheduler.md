# ADR-0003: Externally triggered scheduler, bounded to a 24h window

## Status

Proposal.

## Context

The challenge requires issuing 8-12 Invoices every 3 hours, for 24 hours
total, a bounded run of 8 executions, not an indefinite recurring job.

Cloud Run scales to zero between requests by default. An in-process
scheduled job only fires while an instance is alive; if the instance
scales down between triggers (the normal, cost-free behavior), the
schedule silently stops firing.

## Decision

- The trigger is external: a Cloud Scheduler job (`0 */3 * * *`) sends an
  authenticated `POST /internal/emit-invoices` request every 3 hours. Cloud
  Run wakes up on demand to serve it, runs the emission, and can scale back
  to zero afterward.
- The endpoint is restricted at the IAM layer: only the Cloud Scheduler job's
  service account has the `roles/run.invoker` permission on this route. No
  app-level secret or token check is needed; Cloud Run rejects unauthorized
  callers before the request reaches application code.
- Each execution picks a random count in `[8, 12]`, generates that many fake
  people (name, CPF-shaped tax ID, email) via a `RandomPersonGenerator`.
- Invoices are created through `StarkBankGateway.createInvoices()` first
  (Stark Bank is the source of truth), then persisted locally with the IDs
  and data Stark Bank returns, so the authenticity check in
  [ADR-0002](0002-webhook-security.md) has real emitted invoices to match
  against.
- The 24h bound is enforced in the application: a run-start timestamp is
  persisted in Postgres on the first execution; each invocation checks
  elapsed time and no-ops once 24h have passed.

## Consequences

- No manual step to stop the job after 24h; the app self-terminates the
  emission logic while the Cloud Scheduler job can keep firing harmlessly
  (each call after the 24h mark is a cheap no-op).
- Because the run-start timestamp lives in Postgres (see [ADR-0004](0004-persistence.md)),
  the 24h bound survives Cloud Run scaling instances up and down, or a
  redeploy mid-window.
- Requires provisioning the Cloud Scheduler job and its dedicated service
  account as part of deployment, not just deploying the container.
