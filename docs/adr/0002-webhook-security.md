# ADR-0002: Layered webhook validation (signature, idempotency, authenticity, integrity)

## Status

Proposal.

## Context

Stark Bank's webhook docs (docs.starkbank.com/get-started/webhook)
establish the official contract:

- Every request carries a `Digital-Signature` header. It must be verified
  before the payload is trusted. The Java SDK's `Event.parse(content,
  signature)` handles this internally using the credentials already loaded
  into `Project` - no manual public-key fetch/cache needed on our side
  (confirmed via github.com/starkbank/sdk-java).
- Events may arrive duplicated or out of order. Handlers must be idempotent.
- The recommended flow is: verify signature, persist the raw event, return
  200 fast, process business logic.

Confirmed via the Elixir SDK docs (starkbank.hexdocs.pm), which document
the Invoice.Log lifecycle explicitly: type is one of `created`, `paid`,
`canceled`, `overdue`. The log type that should trigger our Transfer is
`paid`, not `credited` as initially assumed.

On top of that, the original challenge spec requires rejecting: duplicate
deliveries, invoices we never emitted, tampered payloads, and negative
amounts.

## Decision

`ProcessWebhookUseCase` runs a 4-layer pipeline, in this order,
short-circuiting on the first failure:

1. Signature verification (transport authenticity). Validate via
   `StarkBankGateway.parseEvent()`. Invalid signature: reject immediately,
   nothing is persisted.
2. Idempotency (Inbox Pattern). Insert into `webhook_events` with a unique
   constraint on `invoice_id`. A constraint violation means this invoice
   was already processed; short-circuit and return 200 without
   reprocessing.
3. Authenticity (business level). The `invoice_id` from the payload must
   exist in our `invoices` table with status `OPEN` (an invoice we emitted
   and haven't processed yet). Unknown invoice: reject, mark the webhook
   event `REJECTED_UNKNOWN`.
4. Integrity. Recompute `SHA-256(name + taxId + originalAmount)` from our
   stored `Invoice` and compare against the payload. Mismatch, or a
   non-positive amount: reject, mark `REJECTED_INVALID`.

Only after all four layers pass, and only when `event.log.type == "paid"`,
does `CreateTransferUseCase` run.

The webhook endpoint is exempt from CSRF protection (signature verification
already guarantees authenticity, per Stark Bank's own guidance).

## Consequences

- Given the challenge's volume (8-12 invoices per 3h, 24h total, dozens of
  events, not thousands), business logic runs synchronously inside the
  webhook request. No queue is needed for this scale; SQS/DLQ is deferred
  as a stretch goal, not required for correctness here.
- No public-key management on our side: `Event.parse()` handles it via the
  SDK, so there is nothing to fetch or cache at startup for this layer.
- Daily polling of undelivered events (mentioned in the docs as a
  resilience layer for missed deliveries) is left as future work, not
  required for the challenge's 24h window.
