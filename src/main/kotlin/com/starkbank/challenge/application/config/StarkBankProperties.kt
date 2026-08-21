package com.starkbank.challenge.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "starkbank.credentials")
data class StarkBankCredentials(
    val environment: String,
    val projectId: String,
    val privateKey: String,
)

@ConfigurationProperties(prefix = "starkbank.destination")
data class DestinationAccountProperties(
    val bankCode: String,
    val branch: String,
    val account: String,
    val name: String,
    val taxId: String,
    val accountType: String,
)
