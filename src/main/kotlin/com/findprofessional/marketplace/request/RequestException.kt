package com.findprofessional.marketplace.request

import org.springframework.http.HttpStatus

class RequestException(
    message: String,
    val code: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
