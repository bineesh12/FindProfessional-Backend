package com.findprofessional.marketplace.localization

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ContentLanguageFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        response.setHeader("Content-Language", SupportedLocale.normalize(request.getHeader("Accept-Language")))
        filterChain.doFilter(request, response)
    }
}
