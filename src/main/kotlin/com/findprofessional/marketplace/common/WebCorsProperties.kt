package com.findprofessional.marketplace.common

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.web.cors")
data class WebCorsProperties(
    val allowedOrigins: List<String> = listOf("https://getarbio.com")
)
