package com.starkbank.challenge.application.usecase

import com.starkbank.challenge.application.gateway.InvalidSignatureException
import com.starkbank.challenge.application.gateway.ParsedEvent
import com.starkbank.challenge.application.gateway.StarkBankGateway
import com.starkbank.challenge.domain.IntegrityHasher
import com.starkbank.challenge.domain.InvoiceStatus
import com.starkbank.challenge.domain.WebhookEvent
import com.starkbank.challenge.domain.WebhookEventStatus
import com.starkbank.challenge.domain.WebhookOutcome
import com.starkbank.challenge.infra.persistence.InvoiceRepository
import com.starkbank.challenge.infra.persistence.WebhookEventRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

sealed class WebhookHandlingResult {
    data object InvalidSignature : WebhookHandlingResult()

    data object Duplicate : WebhookHandlingResult()

    data class Rejected(
        val outcome: WebhookOutcome,
        val reason: String,
    ) : WebhookHandlingResult()

    data object Accepted : WebhookHandlingResult()
}

private const val PAID_LOG_TYPE = "paid"

@Service
class ProcessWebhookUseCase(
    private val gateway: StarkBankGateway,
    private val invoiceRepository: InvoiceRepository,
    private val webhookEventRepository: WebhookEventRepository,
    private val createTransferUseCase: CreateTransferUseCase,
) {
    @Transactional
    fun handle(
        payload: String,
        signature: String,
    ): WebhookHandlingResult {
        val event =
            try {
                gateway.parseEvent(payload, signature)
            } catch (e: InvalidSignatureException) {
                return WebhookHandlingResult.InvalidSignature
            }

        val inboxEntry = openInboxEntry(event, payload) ?: return WebhookHandlingResult.Duplicate

        val invoice = invoiceRepository.findById(event.invoiceId).orElse(null)
        if (invoice == null || invoice.status != InvoiceStatus.OPEN) {
            reject(inboxEntry, WebhookOutcome.REJECTED_UNKNOWN, "invoice was not emitted by us or is no longer open")
            return WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_UNKNOWN, "unknown invoice")
        }

        val expectedHash = IntegrityHasher.hash(event.name, event.taxId, invoice.amount)
        if (event.amount <= 0 || expectedHash != invoice.integrityHash) {
            reject(inboxEntry, WebhookOutcome.REJECTED_INVALID, "payload does not match the emitted invoice")
            return WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_INVALID, "integrity check failed")
        }

        if (event.logType != PAID_LOG_TYPE) {
            accept(inboxEntry)
            return WebhookHandlingResult.Accepted
        }

        val paidInvoice =
            invoice.copy(
                status = InvoiceStatus.PAID,
                paidAt = LocalDateTime.now(),
                fee = event.fee,
            )
        invoiceRepository.save(paidInvoice)

        try {
            createTransferUseCase.execute(paidInvoice)
        } catch (e: IllegalArgumentException) {
            reject(inboxEntry, WebhookOutcome.REJECTED_TRANSFER_FAILED, e.message ?: "transfer could not be created")
            return WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_TRANSFER_FAILED, "transfer failed")
        }

        accept(inboxEntry)
        return WebhookHandlingResult.Accepted
    }

    private fun openInboxEntry(
        event: ParsedEvent,
        payload: String,
    ): WebhookEvent? =
        try {
            webhookEventRepository.saveAndFlush(
                WebhookEvent(eventId = event.eventId, invoiceId = event.invoiceId, payload = payload),
            )
        } catch (e: DataIntegrityViolationException) {
            null
        }

    private fun reject(
        entry: WebhookEvent,
        outcome: WebhookOutcome,
        reason: String,
    ) {
        webhookEventRepository.save(
            entry.copy(
                status = WebhookEventStatus.PROCESSED,
                outcome = outcome,
                processedAt = LocalDateTime.now(),
                rejectionReason = reason,
            ),
        )
    }

    private fun accept(entry: WebhookEvent) {
        webhookEventRepository.save(
            entry.copy(
                status = WebhookEventStatus.PROCESSED,
                outcome = WebhookOutcome.ACCEPTED,
                processedAt = LocalDateTime.now(),
            ),
        )
    }
}
