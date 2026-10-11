package com.findprofessional.marketplace.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class WebCorsConfigurationTest {
    @Test
    fun `allows configured web origin and required headers`() {
        val source = WebCorsConfiguration(
            WebCorsProperties(listOf("https://www.getarbio.com", "http://localhost:8080"))
        ).corsConfigurationSource()
        val request = MockHttpServletRequest("OPTIONS", "/api/categories")

        val configuration = source.getCorsConfiguration(request)

        assertThat(configuration?.allowedOrigins).containsExactly(
            "https://www.getarbio.com",
            "http://localhost:8080"
        )
        assertThat(configuration?.allowedHeaders).contains(
            "Authorization",
            "Content-Type",
            "Accept-Language"
        )
        assertThat(configuration?.allowCredentials).isTrue()
    }

    @Test
    fun `does not apply web cors policy outside api and websocket routes`() {
        val source = WebCorsConfiguration(WebCorsProperties()).corsConfigurationSource()

        val configuration = source.getCorsConfiguration(MockHttpServletRequest("GET", "/terms"))

        assertThat(configuration).isNull()
    }
}
