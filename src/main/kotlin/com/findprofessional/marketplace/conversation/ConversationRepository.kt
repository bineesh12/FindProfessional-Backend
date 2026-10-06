package com.findprofessional.marketplace.conversation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, UUID> {
    fun findByRequestIdAndProfessionalId(requestId: UUID, professionalId: UUID): Optional<Conversation>

    @Query(
        """
        SELECT conversation FROM Conversation conversation
        WHERE conversation.customerId = :userId OR conversation.professionalId = :userId
        ORDER BY conversation.updatedAt DESC
        """
    )
    fun findAllForParticipant(@Param("userId") userId: UUID): List<Conversation>
}

interface ConversationMessageRepository : JpaRepository<ConversationMessage, UUID> {
    fun findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId: UUID): List<ConversationMessage>
    fun findTopByConversationIdOrderByCreatedAtDescIdDesc(conversationId: UUID): Optional<ConversationMessage>
    fun countByConversationIdAndSenderIdNotAndReadAtIsNull(conversationId: UUID, senderId: UUID): Long

    @Modifying
    @Query(
        """
        UPDATE ConversationMessage message
        SET message.readAt = :readAt
        WHERE message.conversation.id = :conversationId
          AND message.senderId <> :readerId
          AND message.readAt IS NULL
        """
    )
    fun markRead(
        @Param("conversationId") conversationId: UUID,
        @Param("readerId") readerId: UUID,
        @Param("readAt") readAt: Instant
    ): Int
}
