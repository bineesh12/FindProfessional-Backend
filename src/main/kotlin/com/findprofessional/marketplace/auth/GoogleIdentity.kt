package com.findprofessional.marketplace.auth

data class GoogleIdentity(
    val subject: String,
    val email: String,
    val displayName: String?
)
