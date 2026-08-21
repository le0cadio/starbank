package com.starkbank.challenge.application.usecase

import com.starkbank.challenge.application.gateway.CreatedInvoice
import com.starkbank.challenge.application.gateway.CreatedTransfer
import com.starkbank.challenge.application.gateway.InvalidSignatureException
import com.starkbank.challenge.application.gateway.ParsedEvent
import com.starkbank.challenge.application.gateway.PersonData
import com.starkbank.challenge.application.gateway.StarkBankGateway
import com.starkbank.challenge.application.gateway.TransferRequest
import java.util.UUID

class FakeStarkBankGateway : StarkBankGateway {
    var rejectSignature: Boolean = false
    var nextEvent: ParsedEvent? = null
    val createdTransfers = mutableListOf<TransferRequest>()

    override fun createInvoices(people: List<PersonData>): List<CreatedInvoice> =
        people.map {
            CreatedInvoice(id = UUID.randomUUID().toString(), name = it.name, taxId = it.taxId, amount = it.amount)
        }

    override fun createTransfer(request: TransferRequest): CreatedTransfer {
        createdTransfers.add(request)
        return CreatedTransfer(id = UUID.randomUUID().toString(), fee = 0, status = "success")
    }

    override fun parseEvent(
        payload: String,
        signature: String,
    ): ParsedEvent {
        if (rejectSignature) throw InvalidSignatureException("invalid signature in test")
        return nextEvent ?: throw IllegalStateException("nextEvent not configured in fake")
    }
}
