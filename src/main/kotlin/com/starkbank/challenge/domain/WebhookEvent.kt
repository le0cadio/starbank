package com.starkbank.challenge.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "webhook_events", uniqueConstraints = [
    UniqueConstraint(columnNames = ["invoice_id"], name = "uq_webhook_invoice_id")
])
data class WebhookEvent(
    @Id
    val id: String = UUID.randomUUID().toString(),
    
    @Column(name = "invoice_id", nullable = false)
    val invoiceId: String,
    
    @Lob
    @Column(nullable = false)
    val payload: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: WebhookEventStatus = WebhookEventStatus.RECEIVED,
    
    @Enumerated(EnumType.STRING)
    val outcome: WebhookOutcome? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val processedAt: LocalDateTime? = null,
    
    val rejectionReason: String? = null
)

enum class WebhookEventStatus {
    RECEIVED, PROCESSING, PROCESSED, FAILED
}

enum class WebhookOutcome {
    ACCEPTED, REJECTED_UNKNOWN, REJECTED_INVALID
}
