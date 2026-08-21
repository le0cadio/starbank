package com.starkbank.challenge.application.usecase

import com.starkbank.challenge.domain.InvoiceStatus
import com.starkbank.challenge.domain.WebhookOutcome
import com.starkbank.challenge.fixtures.DestinationAccountFixtures
import com.starkbank.challenge.fixtures.InvoiceFixtures
import com.starkbank.challenge.fixtures.ParsedEventFixtures
import com.starkbank.challenge.infra.persistence.InvoiceRepository
import com.starkbank.challenge.infra.persistence.TransferRepository
import com.starkbank.challenge.infra.persistence.WebhookEventRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest
class ProcessWebhookUseCaseTest {
    @Autowired
    private lateinit var invoiceRepository: InvoiceRepository

    @Autowired
    private lateinit var webhookEventRepository: WebhookEventRepository

    @Autowired
    private lateinit var transferRepository: TransferRepository

    private val gateway = FakeStarkBankGateway()

    private lateinit var useCase: ProcessWebhookUseCase

    @BeforeEach
    fun setUp() {
        val createTransferUseCase =
            CreateTransferUseCase(gateway, transferRepository, DestinationAccountFixtures.starkBankAccount())
        useCase = ProcessWebhookUseCase(gateway, invoiceRepository, webhookEventRepository, createTransferUseCase)
    }

    @Test
    fun `accepts a valid paid event and creates a transfer with the net amount`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice())
        gateway.nextEvent = ParsedEventFixtures.paid(invoice)

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Accepted)
        assertThat(gateway.createdTransfers).hasSize(1)
        assertThat(gateway.createdTransfers.first().amount).isEqualTo(invoice.amount - 65)
        assertThat(gateway.createdTransfers.first().externalId).isEqualTo(invoice.id)
        assertThat(invoiceRepository.findById(invoice.id).get().status).isEqualTo(InvoiceStatus.PAID)
        assertThat(webhookEventRepository.findByInvoiceId(invoice.id).single().outcome).isEqualTo(WebhookOutcome.ACCEPTED)
    }

    @Test
    fun `rejects when the signature is invalid and persists nothing`() {
        gateway.rejectSignature = true

        val result = useCase.handle("{}", "bad-sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.InvalidSignature)
        assertThat(webhookEventRepository.count()).isZero()
    }

    @Test
    fun `short circuits a retry of the exact same event id`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice())
        gateway.nextEvent = ParsedEventFixtures.paid(invoice, eventId = "evt-1")
        useCase.handle("{}", "sig")

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Duplicate)
        assertThat(gateway.createdTransfers).hasSize(1)
    }

    @Test
    fun `processes a later distinct event for the same invoice instead of treating it as a duplicate`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice())
        gateway.nextEvent = ParsedEventFixtures.created(invoice, eventId = "evt-created")
        useCase.handle("{}", "sig")

        gateway.nextEvent = ParsedEventFixtures.paid(invoice, eventId = "evt-paid")
        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Accepted)
        assertThat(gateway.createdTransfers).hasSize(1)
        assertThat(webhookEventRepository.findByInvoiceId(invoice.id)).hasSize(2)
    }

    @Test
    fun `rejects an invoice id we never emitted`() {
        val invoice = InvoiceFixtures.openInvoice()
        gateway.nextEvent = ParsedEventFixtures.paid(invoice)

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_UNKNOWN, "unknown invoice"))
        assertThat(gateway.createdTransfers).isEmpty()
    }

    @Test
    fun `rejects a tampered payload where the recomputed hash does not match`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice())
        gateway.nextEvent = ParsedEventFixtures.paid(invoice).copy(name = "Someone Else")

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_INVALID, "integrity check failed"))
        assertThat(gateway.createdTransfers).isEmpty()
    }

    @Test
    fun `rejects a non positive amount even when the hash happens to match`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice())
        gateway.nextEvent = ParsedEventFixtures.paid(invoice).copy(amount = 0)

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_INVALID, "integrity check failed"))
    }

    @Test
    fun `accepts a non paid log type without creating a transfer`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice())
        gateway.nextEvent = ParsedEventFixtures.created(invoice)

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Accepted)
        assertThat(gateway.createdTransfers).isEmpty()
        assertThat(invoiceRepository.findById(invoice.id).get().status).isEqualTo(InvoiceStatus.OPEN)
    }

    @Test
    fun `terminally rejects a paid event whose fee would consume the entire amount instead of retrying forever`() {
        val invoice = invoiceRepository.save(InvoiceFixtures.openInvoice(amount = 100))
        gateway.nextEvent = ParsedEventFixtures.paid(invoice, fee = 100)

        val result = useCase.handle("{}", "sig")

        assertThat(result).isEqualTo(WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_TRANSFER_FAILED, "transfer failed"))
        assertThat(gateway.createdTransfers).isEmpty()
        assertThat(webhookEventRepository.findByInvoiceId(invoice.id).single().outcome)
            .isEqualTo(WebhookOutcome.REJECTED_TRANSFER_FAILED)
    }
}
