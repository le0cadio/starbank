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
    name = "invoices",
    indexes = [
        Index(name = "idx_invoice_status", columnList = "status"),
        Index(name = "idx_invoice_created_at", columnList = "created_at"),
    ],
)
data class Invoice(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false)
    val recipientName: String,
    @Column(nullable = false)
    val recipientEmail: String,
    @Column(nullable = false)
    val recipientCpf: String,
    @Column(nullable = false)
    val amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: InvoiceStatus = InvoiceStatus.OPEN,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val paidAt: LocalDateTime? = null,
    val fee: Long? = null,
    @Column(columnDefinition = "VARCHAR(64)")
    val integrityHash: String? = null,
) {
    companion object {
        fun emit(
            name: String,
            email: String,
            taxId: String,
            amount: Long,
        ): Invoice =
            Invoice(
                recipientName = name,
                recipientEmail = email,
                recipientCpf = taxId,
                amount = amount,
                integrityHash = IntegrityHasher.hash(name, taxId, amount),
            )
    }
}

enum class InvoiceStatus {
    OPEN,
    PAID,
    OVERDUE,
    CANCELED,
}
