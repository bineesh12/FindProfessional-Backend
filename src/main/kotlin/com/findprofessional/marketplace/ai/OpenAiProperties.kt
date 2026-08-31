package com.findprofessional.marketplace.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.ai.openai")
data class OpenAiProperties(
    val apiKey: String = "",
    val model: String = "gpt-5.6-luna",
    val baseUrl: String = "https://api.openai.com/v1",
    val maxOutputTokens: Int = 500,
    val timeoutSeconds: Long = 8
)
