package com.starkbank.challenge.domain

import java.security.MessageDigest

object IntegrityHasher {
    fun hash(
        name: String,
        taxId: String,
        amount: Long,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$name|$taxId|$amount".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
