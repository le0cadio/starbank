package com.starkbank.challenge.application.gateway

data class PersonData(
    val name: String,
    val taxId: String,
    val email: String,
    val amount: Long,
)

data class CreatedInvoice(
    val id: String,
    val name: String,
    val taxId: String,
    val amount: Long,
)

data class TransferRequest(
    val amount: Long,
    val bankCode: String,
    val branchCode: String,
    val accountNumber: String,
    val accountType: String,
    val name: String,
    val taxId: String,
    val externalId: String,
)

data class CreatedTransfer(
    val id: String,
    val fee: Long,
    val status: String,
)

data class ParsedEvent(
    val eventId: String,
    val invoiceId: String,
    val logType: String,
    val name: String,
    val taxId: String,
    val amount: Long,
    val fee: Long,
)

class InvalidSignatureException(
    message: String,
) : RuntimeException(message)

interface StarkBankGateway {
    fun createInvoices(people: List<PersonData>): List<CreatedInvoice>

    fun createTransfer(request: TransferRequest): CreatedTransfer

    fun parseEvent(
        payload: String,
        signature: String,
    ): ParsedEvent
}
