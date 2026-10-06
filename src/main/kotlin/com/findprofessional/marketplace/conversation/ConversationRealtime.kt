package com.findprofessional.marketplace.conversation

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ConversationMessageEvent(
    val type: String = "MESSAGE_CREATED",
    val conversationId: UUID,
    val message: ConversationMessageResponse
)

@Component
class ConversationSocketHandler(
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {
    private val sessionsByUser = ConcurrentHashMap<UUID, MutableSet<WebSocketSession>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.principal?.name?.let(UUID::fromString)
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authenticated user is required"))
            return
        }
        sessionsByUser.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessionsByUser.values.forEach { sessions -> sessions.remove(session) }
        sessionsByUser.entries.removeIf { it.value.isEmpty() }
    }

    fun publish(event: ConversationMessageEvent, participantIds: Set<UUID>) {
        val payload = TextMessage(objectMapper.writeValueAsString(event))
        participantIds.flatMap { sessionsByUser[it].orEmpty() }
            .filter(WebSocketSession::isOpen)
            .forEach { session ->
                runCatching {
                    synchronized(session) {
                        if (session.isOpen) session.sendMessage(payload)
                    }
                }
            }
    }
}

@Configuration
@EnableWebSocket
class ConversationWebSocketConfig(
    private val handler: ConversationSocketHandler
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/conversations").setAllowedOriginPatterns("*")
    }
}
