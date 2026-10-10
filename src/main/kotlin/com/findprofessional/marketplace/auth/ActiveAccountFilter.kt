package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccountRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class ActiveAccountFilter(
    private val users: UserAccountRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? Jwt
        val userId = principal?.subject?.let { subject ->
            runCatching { UUID.fromString(subject) }.getOrNull()
        }
        if (userId != null && !users.existsByIdAndDeletedAtIsNull(userId)) {
            SecurityContextHolder.clearContext()
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        filterChain.doFilter(request, response)
    }
}
