package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.conversation.Conversation
import com.findprofessional.marketplace.conversation.ConversationException
import com.findprofessional.marketplace.conversation.ConversationRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class UserReportReason { SPAM, HARASSMENT, FRAUD, INAPPROPRIATE_CONTENT, OTHER }
enum class UserReportStatus { OPEN, REVIEWED, CLOSED }

@Entity
@Table(
    name = "user_blocks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["blocker_user_id", "blocked_user_id"])]
)
class UserBlock(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "blocker_user_id", nullable = false) val blockerUserId: UUID,
    @Column(name = "blocked_user_id", nullable = false) val blockedUserId: UUID,
    @Column(name = "conversation_id", nullable = false) val conversationId: UUID,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "user_reports")
class UserReport(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "reporter_user_id", nullable = false) val reporterUserId: UUID,
    @Column(name = "reported_user_id", nullable = false) val reportedUserId: UUID,
    @Column(name = "conversation_id", nullable = false) val conversationId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) val reason: UserReportReason,
    @Column(length = 1000) val details: String?,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) val status: UserReportStatus,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
)

interface UserBlockRepository : JpaRepository<UserBlock, UUID> {
    fun existsByBlockerUserIdAndBlockedUserId(blockerUserId: UUID, blockedUserId: UUID): Boolean
    fun deleteByBlockerUserIdAndBlockedUserId(blockerUserId: UUID, blockedUserId: UUID): Long

    @Query(
        """
        SELECT CASE WHEN COUNT(block) > 0 THEN true ELSE false END FROM UserBlock block
        WHERE (block.blockerUserId = :first AND block.blockedUserId = :second)
           OR (block.blockerUserId = :second AND block.blockedUserId = :first)
        """
    )
    fun existsBetween(@Param("first") first: UUID, @Param("second") second: UUID): Boolean
}

interface UserReportRepository : JpaRepository<UserReport, UUID>

data class CreateUserReportRequest(
    val reason: UserReportReason,
    @field:Size(max = 1000) val details: String? = null
)

data class ConversationSafetyResponse(
    val blocked: Boolean,
    val blockedByCurrentUser: Boolean
)

@Service
class UserSafetyService(
    private val conversations: ConversationRepository,
    private val blocks: UserBlockRepository,
    private val reports: UserReportRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional
    fun block(userId: UUID, conversationId: UUID): ConversationSafetyResponse {
        val otherUserId = requireOtherParticipant(userId, conversationId)
        if (!blocks.existsByBlockerUserIdAndBlockedUserId(userId, otherUserId)) {
            blocks.save(UserBlock(blockerUserId = userId, blockedUserId = otherUserId, conversationId = conversationId, createdAt = Instant.now(clock)))
        }
        return ConversationSafetyResponse(blocked = true, blockedByCurrentUser = true)
    }

    @Transactional
    fun unblock(userId: UUID, conversationId: UUID): ConversationSafetyResponse {
        val otherUserId = requireOtherParticipant(userId, conversationId)
        blocks.deleteByBlockerUserIdAndBlockedUserId(userId, otherUserId)
        return ConversationSafetyResponse(
            blocked = blocks.existsBetween(userId, otherUserId),
            blockedByCurrentUser = false
        )
    }

    @Transactional
    fun report(userId: UUID, conversationId: UUID, input: CreateUserReportRequest) {
        val otherUserId = requireOtherParticipant(userId, conversationId)
        reports.save(
            UserReport(
                reporterUserId = userId,
                reportedUserId = otherUserId,
                conversationId = conversationId,
                reason = input.reason,
                details = input.details?.trim()?.ifBlank { null },
                status = UserReportStatus.OPEN,
                createdAt = Instant.now(clock)
            )
        )
    }

    private fun requireOtherParticipant(userId: UUID, conversationId: UUID): UUID {
        val conversation = conversations.findById(conversationId).orElseThrow {
            ConversationException("Conversation was not found", "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        return conversation.otherParticipant(userId)
    }

    private fun Conversation.otherParticipant(userId: UUID): UUID = when (userId) {
        customerId -> professionalId
        professionalId -> customerId
        else -> throw ConversationException(
            "Conversation access is not allowed",
            "CONVERSATION_FORBIDDEN",
            HttpStatus.FORBIDDEN
        )
    }
}
