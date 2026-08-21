package com.starkbank.challenge.web

import com.starkbank.challenge.application.usecase.ProcessWebhookUseCase
import com.starkbank.challenge.application.usecase.WebhookHandlingResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class WebhookController(
    private val processWebhookUseCase: ProcessWebhookUseCase,
) {
    @PostMapping("/webhook")
    fun receive(
        @RequestBody payload: String,
        @RequestHeader("Digital-Signature") signature: String,
    ): ResponseEntity<Unit> {
        val result = processWebhookUseCase.handle(payload, signature)
        return when (result) {
            is WebhookHandlingResult.InvalidSignature -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
            else -> ResponseEntity.ok().build()
        }
    }
}
