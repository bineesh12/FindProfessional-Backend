package com.findprofessional.marketplace.conversation

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import com.findprofessional.marketplace.request.RequestSession
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserBlockRepository
import com.findprofessional.marketplace.notification.NotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class ConversationServiceTest {
    private val conversations = mock(ConversationRepository::class.java)
    private val messages = mock(ConversationMessageRepository::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val users = mock(UserAccountRepository::class.java)
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val realtime = mock(ConversationSocketHandler::class.java)
    private val notifications = mock(NotificationService::class.java)
    private val blocks = mock(UserBlockRepository::class.java)
    private val service = ConversationService(
        conversations, messages, requests, offers, users, profiles, realtime, notifications, blocks
    )

    @Test
    fun `customer can open a conversation for a submitted offer`() {
        val fixture = fixture()
        val createdConversation = fixture.conversation()
        `when`(requests.findByIdForUpdate(fixture.request.id)).thenReturn(Optional.of(fixture.request))
        `when`(offers.findByProfessionalUserIdAndRequestId(fixture.professionalId, fixture.request.id))
            .thenReturn(Optional.of(fixture.offer))
        `when`(conversations.findByRequestIdAndProfessionalId(fixture.request.id, fixture.professionalId))
            .thenReturn(Optional.empty())
        `when`(conversations.save(any(Conversation::class.java))).thenReturn(createdConversation)
        `when`(users.findById(fixture.professionalId)).thenReturn(
            Optional.of(UserAccount(id = fixture.professionalId, displayName = "Nordic Roofing"))
        )
        `when`(profiles.findById(fixture.professionalId)).thenReturn(Optional.empty())
        `when`(messages.findTopByConversationIdOrderByCreatedAtDescIdDesc(createdConversation.id))
            .thenReturn(Optional.empty())
        `when`(
            messages.countByConversationIdAndSenderIdNotAndReadAtIsNull(createdConversation.id, fixture.customerId)
        ).thenReturn(0)

        val response = service.open(
            fixture.customerId,
            OpenConversationRequest(fixture.request.id, fixture.professionalId)
        )

        assertEquals(fixture.request.id, response.requestId)
        assertEquals("Nordic Roofing", response.otherParticipantName)
        assertEquals(true, response.canSendMessages)
    }

    @Test
    fun `user outside the conversation cannot read it`() {
        val fixture = fixture()
        val conversation = fixture.conversation()
        `when`(conversations.findById(conversation.id)).thenReturn(Optional.of(conversation))

        val error = assertThrows(ConversationException::class.java) {
            service.thread(UUID.randomUUID(), conversation.id)
        }

        assertEquals("CONVERSATION_FORBIDDEN", error.code)
    }

    @Test
    fun `sending trims and publishes a message to both participants`() {
        val fixture = fixture()
        val conversation = fixture.conversation()
        `when`(conversations.findById(conversation.id)).thenReturn(Optional.of(conversation))
        `when`(offers.findByProfessionalUserIdAndRequestId(fixture.professionalId, fixture.request.id))
            .thenReturn(Optional.of(fixture.offer))
        `when`(messages.save(any(ConversationMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.send(fixture.customerId, conversation.id, SendConversationMessageRequest("  Hello  "))

        assertEquals("Hello", response.content)
        verify(realtime).publish(
            ConversationMessageEvent(conversationId = conversation.id, message = response),
            setOf(fixture.customerId, fixture.professionalId)
        )
    }

    @Test
    fun `declined offer makes its conversation read only`() {
        val fixture = fixture(offerStatus = ProfessionalOfferStatus.DECLINED)
        val conversation = fixture.conversation()
        `when`(conversations.findById(conversation.id)).thenReturn(Optional.of(conversation))
        `when`(offers.findByProfessionalUserIdAndRequestId(fixture.professionalId, fixture.request.id))
            .thenReturn(Optional.of(fixture.offer))

        val error = assertThrows(ConversationException::class.java) {
            service.send(fixture.customerId, conversation.id, SendConversationMessageRequest("Hello"))
        }

        assertEquals("CONVERSATION_READ_ONLY", error.code)
    }

    private fun fixture(
        requestStatus: CustomerRequestStatus = CustomerRequestStatus.PUBLISHED,
        offerStatus: ProfessionalOfferStatus = ProfessionalOfferStatus.SUBMITTED
    ): Fixture {
        val customerId = UUID.randomUUID()
        val professionalId = UUID.randomUUID()
        val request = CustomerRequest(
            session = mock(RequestSession::class.java),
            customerId = customerId,
            category = mock(ServiceCategory::class.java),
            service = mock(MarketplaceService::class.java),
            status = requestStatus,
            title = "Repair the roof",
            description = "Repair leaking roof tiles."
        )
        val offer = ProfessionalOffer(
            professionalUserId = professionalId,
            request = request,
            amount = BigDecimal("12000"),
            currency = "SEK",
            status = offerStatus
        )
        return Fixture(customerId, professionalId, request, offer)
    }

    private data class Fixture(
        val customerId: UUID,
        val professionalId: UUID,
        val request: CustomerRequest,
        val offer: ProfessionalOffer
    ) {
        fun conversation() = Conversation(
            request = request,
            customerId = customerId,
            professionalId = professionalId
        )
    }
}
