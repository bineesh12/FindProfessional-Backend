package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.ai.AiRequestDraftService
import com.findprofessional.marketplace.auth.anyValue
import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.question.QuestionEngine
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.service.MarketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class RequestSummaryServiceTest {
    private val sessionService = mock(RequestSessionService::class.java)
    private val answers = mock(RequestAnswerRepository::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val sessions = mock(RequestSessionRepository::class.java)
    private val questions = mock(QuestionEngine::class.java)
    private val aiDrafts = mock(AiRequestDraftService::class.java)
    private val locationService = mock(RequestLocationService::class.java)
    private val summaries = RequestSummaryService(
        sessionService,
        answers,
        requests,
        sessions,
        questions,
        aiDrafts,
        locationService
    )

    init {
        doAnswer { it.arguments[0] }.`when`(sessions).save(anyValue())
    }

    @Test
    fun `summary persists deterministic editable draft when AI is unavailable`() {
        val fixture = fixture()
        `when`(sessionService.ownedSession(fixture.userId, fixture.session.id))
            .thenReturn(fixture.session)
        `when`(answers.findAllBySessionId(fixture.session.id)).thenReturn(listOf(fixture.answer))
        `when`(questions.allQuestions(fixture.service.id)).thenReturn(listOf(fixture.question))

        val summary = summaries.summary(fixture.userId, fixture.session.id)

        assertEquals("Appliance repair", summary.title)
        assertTrue(summary.description.contains("Washing machine is leaking"))
        verify(sessions).save(fixture.session)
    }

    @Test
    fun `user draft edits are persisted and returned`() {
        val fixture = fixture()
        `when`(sessionService.ownedSession(fixture.userId, fixture.session.id))
            .thenReturn(fixture.session)
        `when`(answers.findAllBySessionId(fixture.session.id)).thenReturn(listOf(fixture.answer))
        `when`(questions.allQuestions(fixture.service.id)).thenReturn(listOf(fixture.question))

        val summary = summaries.updateDraft(
            fixture.userId,
            fixture.session.id,
            UpdateRequestDraftRequest(
                title = "Repair leaking washing machine",
                description = "The washing machine leaks during every wash cycle."
            )
        )

        assertEquals("Repair leaking washing machine", summary.title)
        assertEquals("The washing machine leaks during every wash cycle.", summary.description)
    }

    private fun fixture(): Fixture {
        val userId = UUID.randomUUID()
        val category = category()
        val service = MarketplaceService(
            category = category,
            code = "APPLIANCE_REPAIR",
            name = "Appliance repair",
            shortDescription = "Repair appliances",
            iconKey = "home_repair_service"
        )
        val question = ServiceQuestion(
            service = service,
            key = "problem_description",
            prompt = "What is happening with the appliance?",
            type = QuestionType.TEXT,
            displayOrder = 1
        )
        val session = RequestSession(
            customerId = userId,
            category = category,
            service = service,
            status = RequestSessionStatus.READY_FOR_REVIEW,
            initialDescription = "Washing machine is leaking during every wash cycle."
        )
        return Fixture(
            userId,
            service,
            question,
            session,
            RequestAnswer(session = session, question = question, value = "Water leaks underneath")
        )
    }

    private data class Fixture(
        val userId: UUID,
        val service: MarketplaceService,
        val question: ServiceQuestion,
        val session: RequestSession,
        val answer: RequestAnswer
    )
}
