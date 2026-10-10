package com.findprofessional.marketplace.conversation

import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserBlockRepository
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Service
class ConversationService(
    private val conversations: ConversationRepository,
    private val messages: ConversationMessageRepository,
    private val requests: CustomerRequestRepository,
    private val offers: ProfessionalOfferRepository,
    private val users: UserAccountRepository,
    private val professionalProfiles: ProfessionalProfileRepository,
    private val realtime: ConversationSocketHandler,
    private val notifications: NotificationService,
    private val blocks: UserBlockRepository
) {
    @Transactional
    fun open(userId: UUID, input: OpenConversationRequest): ConversationSummaryResponse {
        val request = requests.findByIdForUpdate(input.requestId).orElseThrow(::requestNotFound)
        val professionalId = when {
            request.customerId == userId -> input.professionalId ?: throw invalid(
                "Professional is required when a customer starts a conversation",
                "PROFESSIONAL_REQUIRED"
            )
            input.professionalId == null || input.professionalId == userId -> userId
            else -> throw forbidden()
        }
        val offer = requireEligibleOffer(request.id, professionalId)
        val conversation = conversations.findByRequestIdAndProfessionalId(request.id, professionalId)
            .orElseGet {
                conversations.save(
                    Conversation(
                        request = request,
                        customerId = request.customerId,
                        professionalId = professionalId
                    )
                )
            }
        return conversation.toSummary(userId, offer)
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): List<ConversationSummaryResponse> =
        conversations.findAllForParticipant(userId).map { conversation ->
            conversation.requireParticipant(userId)
            conversation.toSummary(
                userId,
                offers.findByProfessionalUserIdAndRequestId(conversation.professionalId, conversation.request.id)
                    .orElse(null)
            )
        }

    @Transactional
    fun thread(userId: UUID, conversationId: UUID): ConversationThreadResponse {
        val conversation = requireConversation(userId, conversationId)
        messages.markRead(conversation.id, userId, Instant.now())
        notifications.markConversationRead(userId, conversation.id)
        val offer = offers.findByProfessionalUserIdAndRequestId(conversation.professionalId, conversation.request.id)
            .orElse(null)
        return ConversationThreadResponse(
            conversation = conversation.toSummary(userId, offer),
            messages = messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversation.id)
                .map { it.toResponse() }
        )
    }

    @Transactional
    fun send(
        userId: UUID,
        conversationId: UUID,
        input: SendConversationMessageRequest
    ): ConversationMessageResponse {
        val conversation = requireConversation(userId, conversationId)
        val offer = offers.findByProfessionalUserIdAndRequestId(conversation.professionalId, conversation.request.id)
            .orElseThrow { forbidden() }
        val otherUserId = if (userId == conversation.customerId) conversation.professionalId else conversation.customerId
        if (blocks.existsBetween(userId, otherUserId)) {
            throw ConversationException("Messages are blocked", "USER_BLOCKED", HttpStatus.CONFLICT)
        }
        if (!conversation.canSend(offer)) {
            throw ConversationException(
                "Messages cannot be sent for this offer",
                "CONVERSATION_READ_ONLY",
                HttpStatus.CONFLICT
            )
        }
        val content = input.content.trim()
        if (content.isEmpty() || content.length > MaximumMessageLength) {
            throw invalid("Message must contain between 1 and $MaximumMessageLength characters", "INVALID_MESSAGE")
        }
        val saved = messages.save(
            ConversationMessage(
                conversation = conversation,
                senderId = userId,
                content = content
            )
        )
        conversation.updatedAt = saved.createdAt
        val response = saved.toResponse()
        notifications.create(
            CreateNotification(
                userId = if (userId == conversation.customerId) conversation.professionalId else conversation.customerId,
                type = MarketplaceNotificationType.MESSAGE_RECEIVED,
                titleKey = "notification.message.title",
                body = content.replace('\n', ' ').take(MessagePreviewLength),
                requestId = conversation.request.id,
                conversationId = conversation.id,
                messageId = saved.id
            )
        )
        publishAfterCommit(conversation, response)
        return response
    }

    private fun requireConversation(userId: UUID, conversationId: UUID): Conversation =
        conversations.findById(conversationId).orElseThrow {
            ConversationException("Conversation was not found", "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND)
        }.also { it.requireParticipant(userId) }

    private fun requireEligibleOffer(requestId: UUID, professionalId: UUID): ProfessionalOffer {
        val offer = offers.findByProfessionalUserIdAndRequestId(professionalId, requestId).orElseThrow { forbidden() }
        if (offer.status !in ConversationOfferStatuses) throw forbidden()
        return offer
    }

    private fun Conversation.requireParticipant(userId: UUID) {
        if (userId != customerId && userId != professionalId) throw forbidden()
    }

    private fun Conversation.toSummary(userId: UUID, offer: ProfessionalOffer?): ConversationSummaryResponse {
        val otherId = if (userId == customerId) professionalId else customerId
        val otherUser = users.findById(otherId).orElseThrow {
            ConversationException("Conversation participant was not found", "PARTICIPANT_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        val otherName = if (otherId == professionalId) {
            professionalProfiles.findById(otherId).map { it.businessName }.orElse(otherUser.displayName)
        } else {
            otherUser.displayName
        }
        val lastMessage = messages.findTopByConversationIdOrderByCreatedAtDescIdDesc(id).orElse(null)
        val blocked = blocks.existsBetween(userId, otherId)
        return ConversationSummaryResponse(
            id = id,
            requestId = request.id,
            requestTitle = request.title,
            otherParticipantId = otherId,
            otherParticipantName = otherName,
            lastMessage = lastMessage?.content,
            lastMessageAt = lastMessage?.createdAt,
            unreadCount = messages.countByConversationIdAndSenderIdNotAndReadAtIsNull(id, userId),
            canSendMessages = offer != null && canSend(offer) && !blocked,
            blocked = blocked,
            blockedByCurrentUser = blocks.existsByBlockerUserIdAndBlockedUserId(userId, otherId)
        )
    }

    private fun Conversation.canSend(offer: ProfessionalOffer): Boolean =
        offer.status == ProfessionalOfferStatus.SUBMITTED && request.status == CustomerRequestStatus.PUBLISHED ||
            offer.status == ProfessionalOfferStatus.ACCEPTED && request.status in AcceptedConversationStatuses

    private fun publishAfterCommit(conversation: Conversation, message: ConversationMessageResponse) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(conversation, message)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                publish(conversation, message)
            }
        })
    }

    private fun publish(conversation: Conversation, message: ConversationMessageResponse) {
        realtime.publish(
            ConversationMessageEvent(conversationId = conversation.id, message = message),
            setOf(conversation.customerId, conversation.professionalId)
        )
    }

    private fun ConversationMessage.toResponse() = ConversationMessageResponse(id, senderId, content, createdAt, readAt)
    private fun requestNotFound() =
        ConversationException("Request was not found", "REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND)
    private fun forbidden() =
        ConversationException("Conversation access is not allowed", "CONVERSATION_FORBIDDEN", HttpStatus.FORBIDDEN)
    private fun invalid(message: String, code: String) = ConversationException(message, code)

    private companion object {
        const val MaximumMessageLength = 4000
        const val MessagePreviewLength = 140
        val ConversationOfferStatuses = setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
        val AcceptedConversationStatuses = setOf(
            CustomerRequestStatus.HIRED,
            CustomerRequestStatus.WORK_FINISHED,
            CustomerRequestStatus.COMPLETED
        )
    }
}
