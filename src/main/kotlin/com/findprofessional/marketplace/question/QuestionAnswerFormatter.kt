package com.findprofessional.marketplace.question

fun ServiceQuestion.displayAnswer(value: String): String =
    options.firstOrNull { it.value == value }?.label
        ?: unit?.takeIf { it.isNotBlank() }?.let { "$value $it" }
        ?: value
