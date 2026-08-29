package com.findprofessional.marketplace.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
    val issuer: String = "find-professional",
    val jwtSecret: String = "",
    val accessTokenMinutes: Long = 15,
    val refreshTokenDays: Long = 30,
    val phoneCodeMinutes: Long = 10,
    val exposePhoneCode: Boolean = false,
    val googleClientIds: Set<String> = emptySet(),
    val firebaseProjectId: String = ""
)
