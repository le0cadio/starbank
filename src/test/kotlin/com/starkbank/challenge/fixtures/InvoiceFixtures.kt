package com.starkbank.challenge.fixtures

import com.starkbank.challenge.domain.Invoice

object InvoiceFixtures {
    fun openInvoice(
        name: String = "Buzz Aldrin",
        email: String = "buzz@example.com",
        taxId: String = "012.345.678-90",
        amount: Long = 23571,
    ): Invoice = Invoice.emit(name, email, taxId, amount)
}
