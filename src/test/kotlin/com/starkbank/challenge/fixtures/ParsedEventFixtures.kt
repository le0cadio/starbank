package com.starkbank.challenge.fixtures

import com.starkbank.challenge.application.gateway.ParsedEvent
import com.starkbank.challenge.domain.Invoice
import java.util.UUID

object ParsedEventFixtures {
    fun paid(
        invoice: Invoice,
        fee: Long = 65,
        eventId: String = UUID.randomUUID().toString(),
    ): ParsedEvent =
        ParsedEvent(
            eventId = eventId,
            invoiceId = invoice.id,
            logType = "paid",
            name = invoice.recipientName,
            taxId = invoice.recipientCpf,
            amount = invoice.amount,
            fee = fee,
        )

    fun created(
        invoice: Invoice,
        eventId: String = UUID.randomUUID().toString(),
    ): ParsedEvent =
        ParsedEvent(
            eventId = eventId,
            invoiceId = invoice.id,
            logType = "created",
            name = invoice.recipientName,
            taxId = invoice.recipientCpf,
            amount = invoice.amount,
            fee = 0,
        )
}
