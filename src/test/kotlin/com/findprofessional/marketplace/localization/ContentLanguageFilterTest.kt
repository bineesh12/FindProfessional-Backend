package com.findprofessional.marketplace.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ContentLanguageFilterTest {
    @Test
    fun `response exposes normalized content language`() {
        val request = MockHttpServletRequest().apply { addHeader("Accept-Language", "sv-SE") }
        val response = MockHttpServletResponse()

        ContentLanguageFilter().doFilter(request, response, MockFilterChain())

        assertEquals("sv", response.getHeader("Content-Language"))
    }
}
