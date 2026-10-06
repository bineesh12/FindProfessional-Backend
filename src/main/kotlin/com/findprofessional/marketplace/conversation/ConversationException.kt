package com.findprofessional.marketplace.conversation

import org.springframework.http.HttpStatus

class ConversationException(
    message: String,
    val code: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
