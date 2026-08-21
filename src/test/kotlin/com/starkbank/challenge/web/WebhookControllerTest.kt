package com.starkbank.challenge.web

import com.starkbank.challenge.application.usecase.ProcessWebhookUseCase
import com.starkbank.challenge.application.usecase.WebhookHandlingResult
import com.starkbank.challenge.domain.WebhookOutcome
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class WebhookControllerTest {
    private val processWebhookUseCase = mockk<ProcessWebhookUseCase>()
    private val controller = WebhookController(processWebhookUseCase)

    @Test
    fun `returns 400 when the use case reports an invalid signature`() {
        every { processWebhookUseCase.handle(any(), any()) } returns WebhookHandlingResult.InvalidSignature

        val response = controller.receive("{}", "bad-sig")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 200 when the use case accepts the event`() {
        every { processWebhookUseCase.handle(any(), any()) } returns WebhookHandlingResult.Accepted

        val response = controller.receive("{}", "sig")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `returns 200 when the use case reports a duplicate delivery`() {
        every { processWebhookUseCase.handle(any(), any()) } returns WebhookHandlingResult.Duplicate

        val response = controller.receive("{}", "sig")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `returns 200 when the use case rejects the event for a business reason`() {
        every { processWebhookUseCase.handle(any(), any()) } returns
            WebhookHandlingResult.Rejected(WebhookOutcome.REJECTED_INVALID, "integrity check failed")

        val response = controller.receive("{}", "sig")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
