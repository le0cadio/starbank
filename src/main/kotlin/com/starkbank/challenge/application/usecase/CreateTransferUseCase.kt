package com.starkbank.challenge.application.usecase

import com.starkbank.challenge.application.config.DestinationAccountProperties
import com.starkbank.challenge.application.gateway.StarkBankGateway
import com.starkbank.challenge.application.gateway.TransferRequest
import com.starkbank.challenge.domain.Invoice
import com.starkbank.challenge.domain.Transfer
import com.starkbank.challenge.domain.TransferStatus
import com.starkbank.challenge.infra.persistence.TransferRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CreateTransferUseCase(
    private val gateway: StarkBankGateway,
    private val transferRepository: TransferRepository,
    private val destination: DestinationAccountProperties,
) {
    fun execute(invoice: Invoice) {
        val netAmount = invoice.amount - (invoice.fee ?: 0)
        require(netAmount > 0) { "net transfer amount must be positive, got $netAmount" }

        val created =
            gateway.createTransfer(
                TransferRequest(
                    amount = netAmount,
                    bankCode = destination.bankCode,
                    branchCode = destination.branch,
                    accountNumber = destination.account,
                    accountType = destination.accountType,
                    name = destination.name,
                    taxId = destination.taxId,
                    externalId = invoice.id,
                ),
            )

        transferRepository.save(
            Transfer(
                sourceInvoiceId = invoice.id,
                starkbankTransferId = created.id,
                recipientBankCode = destination.bankCode,
                recipientBranch = destination.branch,
                recipientAccount = destination.account,
                recipientName = destination.name,
                recipientTaxId = destination.taxId,
                amount = netAmount,
                fee = created.fee,
                status = mapStatus(created.status),
                processedAt = LocalDateTime.now(),
            ),
        )
    }

    private fun mapStatus(starkbankStatus: String): TransferStatus =
        when (starkbankStatus) {
            "success" -> TransferStatus.SUCCESS
            "failed", "canceled" -> TransferStatus.FAILED
            else -> TransferStatus.PENDING
        }
}
