package com.starkbank.challenge.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "transfers",
    indexes = [
        Index(name = "idx_transfer_invoice", columnList = "source_invoice_id"),
        Index(name = "idx_transfer_status", columnList = "status"),
    ],
)
data class Transfer(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "source_invoice_id", nullable = false)
    val sourceInvoiceId: String,
    @Column(name = "starkbank_transfer_id", nullable = false)
    val starkbankTransferId: String,
    @Column(nullable = false)
    val recipientBankCode: String,
    @Column(nullable = false)
    val recipientBranch: String,
    @Column(nullable = false)
    val recipientAccount: String,
    @Column(nullable = false)
    val recipientName: String,
    @Column(nullable = false)
    val recipientTaxId: String,
    @Column(nullable = false)
    val amount: Long,
    @Column(nullable = false)
    val fee: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TransferStatus = TransferStatus.PENDING,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val processedAt: LocalDateTime? = null,
)

enum class TransferStatus {
    PENDING,
    SUCCESS,
    FAILED,
}
