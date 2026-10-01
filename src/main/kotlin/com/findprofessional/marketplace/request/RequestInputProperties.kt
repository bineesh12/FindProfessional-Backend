package com.findprofessional.marketplace.request

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.request-input")
data class RequestInputProperties(
    val supportedCurrencies: List<String> = listOf("SEK", "EUR", "NOK", "DKK", "USD", "GBP")
)
