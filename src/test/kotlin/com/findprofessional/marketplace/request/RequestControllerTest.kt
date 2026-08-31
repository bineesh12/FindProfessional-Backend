package com.findprofessional.marketplace.request

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class RequestControllerTest {
    @Test
    fun `start uses authenticated customer id`() {
        val userId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val sessions = mock(RequestSessionService::class.java)
        val summaries = mock(RequestSummaryService::class.java)
        val request = StartRequestSessionRequest(categoryId)
        val response = mock(RequestSessionResponse::class.java)
        `when`(sessions.start(userId, request)).thenReturn(response)
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject(userId.toString())
            .build()

        val actual = RequestController(sessions, summaries).start(jwt, request)

        assertEquals(response, actual)
    }
}
