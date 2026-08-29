package com.findprofessional.marketplace.auth

import org.springframework.http.HttpStatus

class AuthException(
    message: String,
    val code: String = "AUTH_ERROR",
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
