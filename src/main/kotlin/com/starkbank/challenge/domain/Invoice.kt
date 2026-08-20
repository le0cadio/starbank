package com.starkbank.challenge.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "invoices", indexes = [
    Index(name = "idx_invoice_status", columnList = "status"),
    Index(name = "idx_invoice_created_at", columnList = "created_at")
])
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
)

enum class InvoiceStatus {
    OPEN, PAID, OVERDUE, CANCELED
}
