package com.findprofessional.marketplace.conversation

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class ConversationControllerTest {
    @Test
    fun `send uses authenticated user and conversation ids`() {
        val service = mock(ConversationService::class.java)
        val controller = ConversationController(service)
        val userId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val input = SendConversationMessageRequest("Can you start next week?")
        val expected = ConversationMessageResponse(
            UUID.randomUUID(),
            userId,
            input.content,
            Instant.now(),
            null
        )
        val jwt = Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            mapOf("alg" to "none"),
            mapOf("sub" to userId.toString())
        )
        `when`(service.send(userId, conversationId, input)).thenReturn(expected)

        val response = controller.send(jwt, conversationId, input)

        assertSame(expected, response)
        verify(service).send(userId, conversationId, input)
    }
}
