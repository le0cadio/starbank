package com.starkbank.challenge.fixtures

import com.starkbank.challenge.application.config.DestinationAccountProperties

object DestinationAccountFixtures {
    fun starkBankAccount(): DestinationAccountProperties =
        DestinationAccountProperties(
            bankCode = "20018183",
            branch = "0001",
            account = "6341320293482496",
            name = "Stark Bank S.A.",
            taxId = "20.018.183/0001-80",
            accountType = "payment",
        )
}
