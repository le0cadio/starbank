package com.starkbank.challenge.application.usecase

import com.starkbank.challenge.domain.Invoice
import com.starkbank.challenge.fixtures.DestinationAccountFixtures
import com.starkbank.challenge.fixtures.InvoiceFixtures
import com.starkbank.challenge.infra.persistence.TransferRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest
class CreateTransferUseCaseTest {
    @Autowired
    private lateinit var transferRepository: TransferRepository

    private val gateway = FakeStarkBankGateway()

    private val destination = DestinationAccountFixtures.starkBankAccount()

    private lateinit var useCase: CreateTransferUseCase

    @BeforeEach
    fun setUp() {
        useCase = CreateTransferUseCase(gateway, transferRepository, destination)
    }

    private fun paidInvoice(fee: Long): Invoice = InvoiceFixtures.openInvoice(amount = 23571).copy(fee = fee)

    @Test
    fun `sends the amount minus fee to the fixed destination account`() {
        val invoice = paidInvoice(fee = 65)

        useCase.execute(invoice)

        val request = gateway.createdTransfers.single()
        assertThat(request.amount).isEqualTo(23506)
        assertThat(request.bankCode).isEqualTo(destination.bankCode)
        assertThat(request.accountNumber).isEqualTo(destination.account)
        assertThat(request.externalId).isEqualTo(invoice.id)
        val savedTransfer = transferRepository.findBySourceInvoiceId(invoice.id)
        assertThat(savedTransfer?.amount).isEqualTo(23506)
        assertThat(savedTransfer?.starkbankTransferId).isNotBlank()
    }

    @Test
    fun `rejects a transfer when the fee would consume the entire amount`() {
        val invoice = InvoiceFixtures.openInvoice(amount = 100).copy(fee = 100)

        assertThatThrownBy { useCase.execute(invoice) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(gateway.createdTransfers).isEmpty()
    }
}
