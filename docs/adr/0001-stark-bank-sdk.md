# ADR-0001: Use the official Stark Bank SDK behind a gateway interface

## Status

Proposal (Sandbox account pending - requires company CNPJ, chasing via recruiter).

## Context

The challenge requires creating `Invoice` and `Transfer` resources and
receiving `Event` webhooks from Stark Bank. Stark Bank recommends using an
official SDK, which handles request signing (private key), typed resources,
and webhook signature verification out of the box.

Confirmed via the Java SDK source (github.com/starkbank/sdk-java):

- Gradle coordinate: `implementation("com.starkbank:sdk:2.25.2")`.
- Auth is a plain object, not a network round-trip up front:
  `Project project = new Project("sandbox", projectId, privateKeyContent)`.
- `Invoice.create(list)` and `Transfer.create(list)` take a `List` and
  return the created resources with server-assigned fields (id, status,
  fee, brcode, etc).
- `Event.parse(content, signature)` verifies the `Digital-Signature` header
  internally, using the credentials already loaded into `Project`. No
  manual public-key fetch/cache is needed on our side.

Sandbox is not fully self-service: it requires registering the company's
CNPJ (confirmed on docs.starkbank.com/sandbox). This blocks real credentials
until that registration goes through.

## Decision

Use the official Stark Bank Java SDK instead of hand-rolled REST calls. All
SDK usage is isolated behind a `StarkBankGateway` interface in the
application layer:

- `createInvoices(people: List<PersonData>): List<Invoice>`
- `createTransfer(...): Transfer`
- `parseEvent(payload: String, signature: String): Event`

Use cases depend only on this interface, never on SDK types directly.

## Consequences

- Domain/use-case layer stays testable with a fake/mock gateway, no network
  or credentials required for unit tests.
- The real `StarkBankGateway` implementation (private key, project ID) is
  the only piece blocked on Sandbox access. Everything else can be built
  and tested in isolation first.
- If the Java SDK turns out to be unsuitable, swapping to raw REST calls
  only touches the gateway implementation, not the domain.
