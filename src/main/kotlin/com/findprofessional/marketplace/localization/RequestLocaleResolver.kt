package com.findprofessional.marketplace.localization

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class RequestLocaleResolver(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse
) {
    fun current(): String = SupportedLocale.normalize(request.getHeader("Accept-Language"))

    fun respondWith(languageTag: String) {
        response.setHeader("Content-Language", SupportedLocale.normalize(languageTag))
    }
}
