package com.findprofessional.marketplace.subscription

import org.springframework.http.HttpStatus

class SubscriptionException(
    override val message: String,
    val code: String,
    val status: HttpStatus
) : RuntimeException(message)
