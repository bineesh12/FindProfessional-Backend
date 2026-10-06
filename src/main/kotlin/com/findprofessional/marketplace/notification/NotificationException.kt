package com.findprofessional.marketplace.notification

import org.springframework.http.HttpStatus

class NotificationException(
    override val message: String,
    val code: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
