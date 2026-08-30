package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.ai.AiRequirementService
import com.findprofessional.marketplace.ai.AiResponseValidator
import com.findprofessional.marketplace.ai.RequirementAnalysis
import com.findprofessional.marketplace.auth.anyValue
import com.findprofessional.marketplace.category.ServiceCategoryRepository
import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.question.QuestionEngine
import com.findprofessional.marketplace.question.QuestionOption
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.MarketplaceServiceRepository
import com.findprofessional.marketplace.service.ServiceAliasRepository
import com.findprofessional.marketplace.service.ServiceAlias
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class RequestSessionServiceTest {
    private val sessions = mock(RequestSessionRepository::class.java)
    private val messages = mock(RequestMessageRepository::class.java)
    private val answers = mock(RequestAnswerRepository::class.java)
    private val categories = mock(ServiceCategoryRepository::class.java)
    private val services = mock(MarketplaceServiceRepository::class.java)
    private val aliases = mock(ServiceAliasRepository::class.java)
    private val suggestions = mock(RequestServiceSuggestionRepository::class.java)
    private val questions = mock(QuestionEngine::class.java)
    private val authorization = mock(CustomerAuthorizationService::class.java)
    private val ai = object : AiRequirementService {
        override fun analyze(request: com.findprofessional.marketplace.ai.RequirementAnalysisRequest) =
            RequirementAnalysis()
    }
    private val service = RequestSessionService(
        sessions,
        messages,
        answers,
        categories,
        services,
        aliases,
        suggestions,
        questions,
        RequirementClassifier(),
        ai,
        AiResponseValidator(),
        authorization
    )

    init {
        doAnswer { it.arguments[0] }.`when`(sessions).save(anyValue())
        doAnswer { it.arguments[0] }.`when`(messages).save(anyValue())
    }

    @Test
    fun `category entry starts with description step`() {
        val userId = UUID.randomUUID()
        val category = category()
        `when`(categories.findById(category.id)).thenReturn(Optional.of(category))
        `when`(messages.findAllBySessionIdOrderBySequenceNumberAsc(anyValue())).thenReturn(emptyList())

        val response = service.start(userId, StartRequestSessionRequest(category.id))

        assertEquals(RequestSessionStatus.NEEDS_DESCRIPTION, response.status)
        assertEquals(RequestStepType.DESCRIPTION, response.currentStep.type)
    }

    @Test
    fun `invalid option is rejected before answer persistence`() {
        val userId = UUID.randomUUID()
        val category = category()
        val marketplaceService = MarketplaceService(
            category = category,
            code = "HOUSE_CONSTRUCTION",
            name = "House construction",
            shortDescription = "Build a home",
            iconKey = "construction"
        )
        val question = ServiceQuestion(
            service = marketplaceService,
            key = "delivery_model",
            prompt = "Delivery model?",
            type = QuestionType.SINGLE_CHOICE,
            displayOrder = 1
        )
        question.options += QuestionOption(
            question = question,
            value = "ONE_COMPANY",
            label = "One company",
            displayOrder = 1
        )
        val session = RequestSession(
            customerId = userId,
            category = category,
            service = marketplaceService,
            status = RequestSessionStatus.COLLECTING_ANSWERS,
            currentQuestion = question
        )
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(questions.allQuestions(marketplaceService.id)).thenReturn(listOf(question))

        val error = assertThrows(RequestException::class.java) {
            service.answer(userId, session.id, question.key, AnswerQuestionRequest("UNKNOWN"))
        }

        assertEquals("INVALID_ANSWER", error.code)
    }

    @Test
    fun `repeated committed answer returns current session without another message`() {
        val userId = UUID.randomUUID()
        val category = category()
        val marketplaceService = MarketplaceService(
            category = category,
            code = "HOUSE_CONSTRUCTION",
            name = "House construction",
            shortDescription = "Build a home",
            iconKey = "construction"
        )
        val question = ServiceQuestion(
            service = marketplaceService,
            key = "land_owned",
            prompt = "Do you own the land?",
            type = QuestionType.BOOLEAN,
            displayOrder = 2
        )
        val session = RequestSession(
            customerId = userId,
            category = category,
            service = marketplaceService,
            status = RequestSessionStatus.READY_FOR_REVIEW
        )
        val persistedAnswer = RequestAnswer(session = session, question = question, value = "TRUE")
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(questions.allQuestions(marketplaceService.id)).thenReturn(listOf(question))
        `when`(answers.findBySessionIdAndQuestionId(session.id, question.id))
            .thenReturn(Optional.of(persistedAnswer))
        `when`(messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id)).thenReturn(emptyList())

        val response = service.answer(
            userId,
            session.id,
            question.key,
            AnswerQuestionRequest("true")
        )

        assertEquals(RequestStepType.SUMMARY, response.currentStep.type)
        verify(messages, never()).save(anyValue())
    }

    @Test
    fun `editing a saved answer reopens its question`() {
        val userId = UUID.randomUUID()
        val category = category()
        val marketplaceService = MarketplaceService(
            category = category,
            code = "HOUSE_CONSTRUCTION",
            name = "House construction",
            shortDescription = "Build a home",
            iconKey = "construction"
        )
        val question = ServiceQuestion(
            service = marketplaceService,
            key = "preferred_start_date",
            prompt = "When should construction begin?",
            type = QuestionType.DATE,
            displayOrder = 6
        )
        val session = RequestSession(
            customerId = userId,
            category = category,
            service = marketplaceService,
            status = RequestSessionStatus.READY_FOR_REVIEW
        )
        val persistedAnswer = RequestAnswer(
            session = session,
            question = question,
            value = "2027-01-01"
        )
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(questions.allQuestions(marketplaceService.id)).thenReturn(listOf(question))
        `when`(answers.findBySessionIdAndQuestionId(session.id, question.id))
            .thenReturn(Optional.of(persistedAnswer))
        `when`(answers.findAllBySessionId(session.id)).thenReturn(listOf(persistedAnswer))
        `when`(messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id)).thenReturn(emptyList())

        val response = service.editAnswer(userId, session.id, question.key)

        assertEquals(RequestSessionStatus.COLLECTING_ANSWERS, response.status)
        assertEquals(question.key, response.currentStep.questionKey)
    }

    @Test
    fun `manual service selection includes other fallback`() {
        val userId = UUID.randomUUID()
        val selectedCategory = category(code = "HOME", name = "Home")
        val otherCategory = category(code = "OTHER", name = "Other")
        val generalHelp = MarketplaceService(
            category = otherCategory,
            code = "GENERAL_HELP",
            name = "General help",
            shortDescription = "Tell us what you need done",
            iconKey = "handyman"
        )
        val session = RequestSession(
            customerId = userId,
            category = selectedCategory,
            status = RequestSessionStatus.NEEDS_SERVICE
        )
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(services.findAllByCategoryIdAndActiveTrueOrderByNameAsc(selectedCategory.id))
            .thenReturn(emptyList())
        `when`(services.findByCodeAndActiveTrue("GENERAL_HELP")).thenReturn(Optional.of(generalHelp))
        `when`(answers.findAllBySessionId(session.id)).thenReturn(emptyList())
        `when`(messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id)).thenReturn(emptyList())

        val response = service.get(userId, session.id)

        assertEquals("Other / Not listed", response.currentStep.options.single().label)
        assertEquals(generalHelp.id.toString(), response.currentStep.options.single().value)
    }

    @Test
    fun `selecting other fallback moves request to general help category`() {
        val userId = UUID.randomUUID()
        val selectedCategory = category(code = "HOME", name = "Home")
        val otherCategory = category(code = "OTHER", name = "Other")
        val generalHelp = MarketplaceService(
            category = otherCategory,
            code = "GENERAL_HELP",
            name = "General help",
            shortDescription = "Tell us what you need done",
            iconKey = "handyman"
        )
        val question = ServiceQuestion(
            service = generalHelp,
            key = "task_details",
            prompt = "What do you need help with?",
            type = QuestionType.TEXT,
            displayOrder = 1
        )
        val session = RequestSession(
            customerId = userId,
            category = selectedCategory,
            status = RequestSessionStatus.NEEDS_SERVICE
        )
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(services.findById(generalHelp.id)).thenReturn(Optional.of(generalHelp))
        `when`(answers.findAllBySessionId(session.id)).thenReturn(emptyList())
        `when`(questions.nextQuestion(generalHelp.id, emptyMap())).thenReturn(question)
        `when`(messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id)).thenReturn(emptyList())

        val response = service.selectService(
            userId,
            session.id,
            SelectRequestServiceRequest(generalHelp.id)
        )

        assertEquals("OTHER", response.category.code)
        assertEquals("GENERAL_HELP", response.service?.code)
        assertEquals("task_details", response.currentStep.questionKey)
    }

    @Test
    fun `manual selection rejects unrelated cross category service`() {
        val userId = UUID.randomUUID()
        val selectedCategory = category(code = "HOME", name = "Home")
        val vehicleCategory = category(code = "VEHICLE", name = "Vehicle")
        val carRepair = MarketplaceService(
            category = vehicleCategory,
            code = "CAR_REPAIR",
            name = "Car repair",
            shortDescription = "Repair a vehicle",
            iconKey = "car_repair"
        )
        val session = RequestSession(
            customerId = userId,
            category = selectedCategory,
            status = RequestSessionStatus.NEEDS_SERVICE
        )
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(services.findById(carRepair.id)).thenReturn(Optional.of(carRepair))

        val error = assertThrows(RequestException::class.java) {
            service.selectService(
                userId,
                session.id,
                SelectRequestServiceRequest(carRepair.id)
            )
        }

        assertEquals("SERVICE_NOT_FOUND", error.code)
    }

    @Test
    fun `matched category description becomes task details and advances`() {
        val userId = UUID.randomUUID()
        val garden = category(code = "GARDEN", name = "Garden")
        val gardenMaintenance = MarketplaceService(
            category = garden,
            code = "GARDEN_MAINTENANCE",
            name = "Garden maintenance",
            shortDescription = "Seasonal garden care",
            iconKey = "yard",
            searchKeywords = "garden grass lawn mowing"
        )
        val taskDetails = ServiceQuestion(
            service = gardenMaintenance,
            key = "task_details",
            prompt = "What do you need help with?",
            type = QuestionType.TEXT,
            displayOrder = 1
        )
        val preferredDate = ServiceQuestion(
            service = gardenMaintenance,
            key = "preferred_date",
            prompt = "When should the work happen?",
            type = QuestionType.DATE,
            displayOrder = 2
        )
        val session = RequestSession(
            customerId = userId,
            category = garden,
            status = RequestSessionStatus.NEEDS_DESCRIPTION
        )
        var savedAnswer: RequestAnswer? = null
        doAnswer {
            (it.arguments[0] as RequestAnswer).also { answer -> savedAnswer = answer }
        }.`when`(answers).save(anyValue())
        `when`(sessions.findByIdAndCustomerId(session.id, userId)).thenReturn(Optional.of(session))
        `when`(services.findAllByCategoryIdAndActiveTrueOrderByNameAsc(garden.id))
            .thenReturn(listOf(gardenMaintenance))
        `when`(aliases.findAllByServiceIdInAndActiveTrue(listOf(gardenMaintenance.id)))
            .thenReturn(
                listOf(
                    ServiceAlias(
                        service = gardenMaintenance,
                        phrase = "garden grass",
                        weight = 100
                    )
                )
            )
        `when`(questions.allQuestions(gardenMaintenance.id)).thenReturn(listOf(taskDetails, preferredDate))
        `when`(answers.findBySessionIdAndQuestionId(session.id, taskDetails.id))
            .thenReturn(Optional.empty())
        `when`(answers.findAllBySessionId(session.id)).thenReturn(emptyList())
        `when`(messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id)).thenReturn(emptyList())
        `when`(
            questions.nextQuestion(
                gardenMaintenance.id,
                mapOf("task_details" to "I need to cut my garden grass")
            )
        ).thenReturn(preferredDate)

        val response = service.submitDescription(
            userId,
            session.id,
            SubmitDescriptionRequest("I need to cut my garden grass")
        )

        assertEquals("I need to cut my garden grass", savedAnswer?.value)
        assertEquals("preferred_date", response.currentStep.questionKey)
    }
}
