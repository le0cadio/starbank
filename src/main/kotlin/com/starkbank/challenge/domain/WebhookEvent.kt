package com.starkbank.challenge.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "webhook_events",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["event_id"], name = "uq_webhook_event_id"),
    ],
)
data class WebhookEvent(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "event_id", nullable = false)
    val eventId: String,
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
    val rejectionReason: String? = null,
)

enum class WebhookEventStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED,
}

enum class WebhookOutcome {
    ACCEPTED,
    REJECTED_UNKNOWN,
    REJECTED_INVALID,
    REJECTED_TRANSFER_FAILED,
}
