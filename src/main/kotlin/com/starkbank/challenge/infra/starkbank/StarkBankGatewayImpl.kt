package com.starkbank.challenge.infra.starkbank

import com.starkbank.Event
import com.starkbank.Project
import com.starkbank.challenge.application.config.StarkBankCredentials
import com.starkbank.challenge.application.gateway.CreatedInvoice
import com.starkbank.challenge.application.gateway.CreatedTransfer
import com.starkbank.challenge.application.gateway.InvalidSignatureException
import com.starkbank.challenge.application.gateway.ParsedEvent
import com.starkbank.challenge.application.gateway.PersonData
import com.starkbank.challenge.application.gateway.StarkBankGateway
import com.starkbank.challenge.application.gateway.TransferRequest
import com.starkbank.error.InvalidSignatureError
import org.springframework.stereotype.Component
import com.starkbank.Invoice as SdkInvoice
import com.starkbank.Transfer as SdkTransfer

@Component
class StarkBankGatewayImpl(
    private val credentials: StarkBankCredentials,
) : StarkBankGateway {
    private val project: Project by lazy {
        Project(credentials.environment, credentials.projectId, credentials.privateKey)
    }

    override fun createInvoices(people: List<PersonData>): List<CreatedInvoice> {
        val requests =
            people.map { person ->
                SdkInvoice(
                    mapOf(
                        "amount" to person.amount,
                        "name" to person.name,
                        "taxId" to person.taxId,
                    ),
                )
            }
        return SdkInvoice.create(requests, project).map {
            CreatedInvoice(
                id = it.id,
                name = it.name,
                taxId = it.taxId,
                amount = it.amount.toLong(),
            )
        }
    }

    override fun createTransfer(request: TransferRequest): CreatedTransfer {
        val sdkTransfer =
            SdkTransfer(
                mapOf(
                    "amount" to request.amount,
                    "bankCode" to request.bankCode,
                    "branchCode" to request.branchCode,
                    "accountNumber" to request.accountNumber,
                    "accountType" to request.accountType,
                    "name" to request.name,
                    "taxId" to request.taxId,
                    "externalId" to request.externalId,
                ),
            )
        val created = SdkTransfer.create(listOf(sdkTransfer), project).first()
        return CreatedTransfer(
            id = created.id,
            fee = (created.fee ?: 0).toLong(),
            status = created.status,
        )
    }

    override fun parseEvent(
        payload: String,
        signature: String,
    ): ParsedEvent {
        val event =
            try {
                Event.parse(payload, signature, project)
            } catch (e: InvalidSignatureError) {
                throw InvalidSignatureException("webhook signature failed verification")
            }

        require(event is Event.InvoiceEvent) {
            "unsupported event subscription: ${event.subscription}"
        }

        val log = event.log
        val invoice = log.invoice
        return ParsedEvent(
            eventId = event.id,
            invoiceId = invoice.id,
            logType = log.type,
            name = invoice.name,
            taxId = invoice.taxId,
            amount = invoice.amount.toLong(),
            fee = (invoice.fee ?: 0).toLong(),
        )
    }
}
