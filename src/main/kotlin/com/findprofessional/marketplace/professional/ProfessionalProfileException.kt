package com.findprofessional.marketplace.professional

import org.springframework.http.HttpStatus

class ProfessionalProfileException(
    message: String,
    val code: String,
    val status: HttpStatus
) : RuntimeException(message)
