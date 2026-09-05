package com.findprofessional.marketplace.matching

import org.springframework.http.HttpStatus

class ProfessionalOpportunityException(
    message: String,
    val code: String,
    val status: HttpStatus
) : RuntimeException(message)
