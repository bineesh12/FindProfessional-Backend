package com.findprofessional.marketplace.localization

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class LocalizedTextService(private val messages: MessageSource) {
    fun get(key: String, locale: String, vararg arguments: Any): String =
        messages.getMessage(key, arguments, Locale.forLanguageTag(SupportedLocale.normalize(locale)))
}
