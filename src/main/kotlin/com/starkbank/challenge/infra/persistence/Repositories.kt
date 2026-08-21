package com.starkbank.challenge.infra.persistence

import com.starkbank.challenge.domain.Invoice
import com.starkbank.challenge.domain.InvoiceStatus
import com.starkbank.challenge.domain.Transfer
import com.starkbank.challenge.domain.TransferStatus
import com.starkbank.challenge.domain.WebhookEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceRepository : JpaRepository<Invoice, String> {
    fun findByStatus(status: InvoiceStatus): List<Invoice>

    fun countByStatus(status: InvoiceStatus): Long
}

@Repository
interface TransferRepository : JpaRepository<Transfer, String> {
    fun findBySourceInvoiceId(sourceInvoiceId: String): Transfer?

    fun findByStatus(status: TransferStatus): List<Transfer>

    fun countByStatus(status: TransferStatus): Long
}

@Repository
interface WebhookEventRepository : JpaRepository<WebhookEvent, String> {
    fun findByEventId(eventId: String): WebhookEvent?

    fun findByInvoiceId(invoiceId: String): List<WebhookEvent>
}
