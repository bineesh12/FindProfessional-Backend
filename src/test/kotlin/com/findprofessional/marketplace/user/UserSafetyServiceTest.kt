package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.conversation.Conversation
import com.findprofessional.marketplace.conversation.ConversationException
import com.findprofessional.marketplace.conversation.ConversationRepository
import com.findprofessional.marketplace.request.CustomerRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class UserSafetyServiceTest {
    private val conversations = mock(ConversationRepository::class.java)
    private val blocks = mock(UserBlockRepository::class.java)
    private val reports = mock(UserReportRepository::class.java)
    private val service = UserSafetyService(conversations, blocks, reports)

    @Test
    fun `participant can block the other participant`() {
        val fixture = fixture()
        `when`(conversations.findById(fixture.conversation.id)).thenReturn(Optional.of(fixture.conversation))

        val response = service.block(fixture.customerId, fixture.conversation.id)

        assertEquals(true, response.blocked)
        assertEquals(true, response.blockedByCurrentUser)
        verify(blocks).save(any(UserBlock::class.java))
    }

    @Test
    fun `participant can report the other participant`() {
        val fixture = fixture()
        `when`(conversations.findById(fixture.conversation.id)).thenReturn(Optional.of(fixture.conversation))

        service.report(
            fixture.customerId,
            fixture.conversation.id,
            CreateUserReportRequest(UserReportReason.FRAUD, "Suspicious payment request")
        )

        verify(reports).save(any(UserReport::class.java))
    }

    @Test
    fun `non participant cannot report a conversation`() {
        val fixture = fixture()
        `when`(conversations.findById(fixture.conversation.id)).thenReturn(Optional.of(fixture.conversation))

        val error = assertThrows(ConversationException::class.java) {
            service.report(UUID.randomUUID(), fixture.conversation.id, CreateUserReportRequest(UserReportReason.SPAM))
        }

        assertEquals("CONVERSATION_FORBIDDEN", error.code)
    }

    private fun fixture(): Fixture {
        val customerId = UUID.randomUUID()
        val professionalId = UUID.randomUUID()
        val request = mock(CustomerRequest::class.java)
        return Fixture(
            customerId,
            Conversation(request = request, customerId = customerId, professionalId = professionalId)
        )
    }

    private data class Fixture(val customerId: UUID, val conversation: Conversation)
}
