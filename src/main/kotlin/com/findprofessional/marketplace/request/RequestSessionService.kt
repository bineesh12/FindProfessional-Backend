package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.ai.AiRequirementService
import com.findprofessional.marketplace.ai.AiResponseValidator
import com.findprofessional.marketplace.ai.RequirementAnalysisRequest
import com.findprofessional.marketplace.ai.RequirementCandidate
import com.findprofessional.marketplace.category.ServiceCategoryRepository
import com.findprofessional.marketplace.question.QuestionEngine
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.MarketplaceServiceRepository
import com.findprofessional.marketplace.service.ServiceAliasRepository
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.springframework.http.HttpStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RequestSessionService(
    private val sessions: RequestSessionRepository,
    private val messages: RequestMessageRepository,
    private val answers: RequestAnswerRepository,
    private val categories: ServiceCategoryRepository,
    private val services: MarketplaceServiceRepository,
    private val aliases: ServiceAliasRepository,
    private val suggestions: RequestServiceSuggestionRepository,
    private val questionEngine: QuestionEngine,
    private val classifier: RequirementClassifier,
    private val aiRequirementService: AiRequirementService,
    private val aiValidator: AiResponseValidator,
    private val authorization: CustomerAuthorizationService
) {
    @Transactional
    fun start(userId: UUID, request: StartRequestSessionRequest): RequestSessionResponse {
        authorization.requireCustomer(userId)
        val category = categories.findById(request.categoryId).orElseThrow {
            notFound("Service category was not found", "CATEGORY_NOT_FOUND")
        }
        if (!category.active) throw notFound("Service category was not found", "CATEGORY_NOT_FOUND")

        val selectedService = request.serviceId?.let { findService(category.id, it) }
        val session = sessions.save(
            RequestSession(
                customerId = userId,
                category = category,
                service = selectedService,
                status = if (selectedService == null) {
                    RequestSessionStatus.NEEDS_DESCRIPTION
                } else {
                    RequestSessionStatus.COLLECTING_ANSWERS
                }
            )
        )
        addMessage(
            session,
            MessageSender.ASSISTANT,
            selectedService?.let { "Let's create your ${it.name.lowercase()} request." }
                ?: "Tell me what you need and I will help structure the request."
        )
        if (selectedService != null) advance(session)
        return response(session)
    }

    @Transactional(readOnly = true)
    fun get(userId: UUID, sessionId: UUID): RequestSessionResponse =
        response(ownedSession(userId, sessionId))

    @Transactional
    fun submitDescription(
        userId: UUID,
        sessionId: UUID,
        request: SubmitDescriptionRequest
    ): RequestSessionResponse {
        val session = ownedSession(userId, sessionId)
        val description = request.description.trim()
        if (session.status != RequestSessionStatus.NEEDS_DESCRIPTION) {
            if (session.initialDescription == description) return response(session)
            throw RequestException("This session is not waiting for a description", "INVALID_REQUEST_STATE")
        }
        session.initialDescription = description
        addMessage(session, MessageSender.USER, description)

        val candidates = services.findAllByCategoryIdAndActiveTrueOrderByNameAsc(session.category.id)
        val rankedMatches = classifier.rank(
            description,
            candidates,
            aliases.findAllByServiceIdInAndActiveTrue(candidates.map(MarketplaceService::id))
        )
        val selectedService = rankedMatches.firstOrNull()
            ?.takeIf { it.confidence == MatchConfidence.HIGH }
            ?.service

        persistSuggestions(
            session,
            rankedSuggestions(description, session.category.code, candidates, rankedMatches),
            selectedService?.id
        )

        if (selectedService == null) {
            session.status = RequestSessionStatus.NEEDS_SERVICE
            addMessage(session, MessageSender.ASSISTANT, "Choose the service that best matches your request.")
        } else {
            session.service = selectedService
            val seededAnswers = seedInitialDescription(session, selectedService)
            addMessage(
                session,
                MessageSender.ASSISTANT,
                "I matched this with ${selectedService.name}. I can help you find the right professionals."
            )
            advance(session, seededAnswers)
        }
        session.touch()
        return response(sessions.save(session))
    }

    @Transactional
    fun selectService(
        userId: UUID,
        sessionId: UUID,
        request: SelectRequestServiceRequest
    ): RequestSessionResponse {
        val session = ownedSession(userId, sessionId)
        if (session.status != RequestSessionStatus.NEEDS_SERVICE) {
            if (session.service?.id == request.serviceId) return response(session)
            throw RequestException("This session is not waiting for a service", "INVALID_REQUEST_STATE")
        }
        val selectedService = findSelectableService(session, request.serviceId)
        val rankedSuggestions = suggestions.findAllBySessionIdOrderByRankAsc(session.id)
        rankedSuggestions.forEach {
            it.selected = it.service.id == selectedService.id
            suggestions.save(it)
        }
        if (rankedSuggestions.none { it.service.id == selectedService.id }) {
            suggestions.save(
                RequestServiceSuggestion(
                    session = session,
                    service = selectedService,
                    score = 0,
                    confidence = MatchConfidence.LOW,
                    source = MatchSource.DETERMINISTIC,
                    rank = rankedSuggestions.size + 1,
                    selected = true
                )
            )
        }
        if (selectedService.code == GeneralHelpServiceCode) {
            session.category = selectedService.category
        }
        session.service = selectedService
        val seededAnswers = seedInitialDescription(session, selectedService)
        addMessage(session, MessageSender.USER, selectedService.name)
        addMessage(session, MessageSender.ASSISTANT, "Great. I can help you find the right professionals.")
        advance(session, seededAnswers)
        session.touch()
        return response(sessions.save(session))
    }

    @Transactional
    fun answer(
        userId: UUID,
        sessionId: UUID,
        questionKey: String,
        request: AnswerQuestionRequest
    ): RequestSessionResponse {
        val session = ownedSession(userId, sessionId)
        val service = session.service
            ?: throw RequestException("This session is not waiting for an answer", "INVALID_REQUEST_STATE")
        val question = questionEngine.allQuestions(service.id).firstOrNull { it.key == questionKey }
            ?: throw RequestException("The question was not found", "QUESTION_NOT_FOUND", HttpStatus.NOT_FOUND)
        val normalized = normalizeAnswer(question, request.value)
        if (session.status != RequestSessionStatus.COLLECTING_ANSWERS ||
            session.currentQuestion?.id != question.id
        ) {
            val existing = answers.findBySessionIdAndQuestionId(session.id, question.id)
            if (existing.isPresent && existing.get().value == normalized) return response(session)
            throw RequestException("The question is no longer active", "QUESTION_NOT_ACTIVE")
        }

        val answer = answers.findBySessionIdAndQuestionId(session.id, question.id)
            .orElseGet { RequestAnswer(session = session, question = question, value = normalized) }
        answer.value = normalized
        answer.updatedAt = Instant.now()
        answers.save(answer)
        saveAnswerTranscript(session, question, normalized)
        advance(session)
        session.touch()
        return response(sessions.save(session))
    }

    @Transactional
    fun editAnswer(userId: UUID, sessionId: UUID, questionKey: String): RequestSessionResponse {
        val session = ownedSession(userId, sessionId)
        if (session.status == RequestSessionStatus.CONFIRMED) {
            throw RequestException("A published request cannot be edited", "REQUEST_ALREADY_CONFIRMED")
        }
        val service = session.service
            ?: throw RequestException("This session has no selected service", "INVALID_REQUEST_STATE")
        val question = questionEngine.allQuestions(service.id).firstOrNull { it.key == questionKey }
            ?: throw RequestException("The question was not found", "QUESTION_NOT_FOUND", HttpStatus.NOT_FOUND)
        if (answers.findBySessionIdAndQuestionId(session.id, question.id).isEmpty) {
            throw RequestException("The answer was not found", "ANSWER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        session.currentQuestion = question
        session.status = RequestSessionStatus.COLLECTING_ANSWERS
        session.touch()
        return response(sessions.save(session))
    }

    @Transactional(readOnly = true)
    fun ownedSession(userId: UUID, sessionId: UUID): RequestSession {
        authorization.requireCustomer(userId)
        return sessions.findByIdAndCustomerId(sessionId, userId).orElseThrow {
            notFound("Request session was not found", "REQUEST_SESSION_NOT_FOUND")
        }
    }

    private fun advance(
        session: RequestSession,
        additionalAnswers: Map<String, String> = emptyMap()
    ) {
        val service = checkNotNull(session.service)
        val answerMap = answers.findAllBySessionId(session.id)
            .associate { it.question.key to it.value } + additionalAnswers
        val nextQuestion = questionEngine.nextQuestion(service.id, answerMap)
        session.currentQuestion = nextQuestion
        session.status = if (nextQuestion == null) {
            RequestSessionStatus.READY_FOR_REVIEW
        } else {
            RequestSessionStatus.COLLECTING_ANSWERS
        }
    }

    private fun response(session: RequestSession): RequestSessionResponse {
        val sessionSuggestions = suggestions.findAllBySessionIdOrderByRankAsc(session.id)
        val availableServices = if (session.status == RequestSessionStatus.NEEDS_SERVICE) {
            selectableServices(session, sessionSuggestions)
        } else {
            emptyList()
        }
        val answerValues = answers.findAllBySessionId(session.id)
            .associate { it.question.key to it.value }
        return RequestSessionResponse(
            id = session.id,
            status = session.status,
            category = session.category.toContext(),
            service = session.service?.toContext(),
            messages = messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id).map {
                RequestMessageResponse(
                    sender = it.sender,
                    content = it.content,
                    questionKey = it.questionKey,
                    answerValue = it.questionKey?.let(answerValues::get)
                        ?.takeIf { _ -> it.sender == MessageSender.USER },
                    editable = it.sender == MessageSender.USER &&
                        it.questionKey != null &&
                        session.status != RequestSessionStatus.CONFIRMED
                )
            },
            currentStep = currentStep(session, availableServices),
            match = sessionSuggestions.firstOrNull { it.selected }
                ?.toResponse()
                ?: sessionSuggestions.firstOrNull()?.toResponse(),
            version = session.version
        )
    }

    private fun currentStep(
        session: RequestSession,
        availableServices: List<MarketplaceService>
    ): RequestStepResponse = when (session.status) {
        RequestSessionStatus.NEEDS_DESCRIPTION -> RequestStepResponse(
            type = RequestStepType.DESCRIPTION,
            questionKey = InitialDescriptionKey,
            title = "What do you need help with in ${session.category.name}?",
            helperText = "Describe the result you want. You do not need to know the professional terminology.",
            answerType = QuestionType.TEXT
        )
        RequestSessionStatus.NEEDS_SERVICE -> RequestStepResponse(
            type = RequestStepType.SERVICE_SELECTION,
            questionKey = ServiceSelectionKey,
            title = "Which service is the closest match?",
            helperText = "Choose the best match. You can refine the details in the next steps.",
            answerType = QuestionType.SINGLE_CHOICE,
            options = availableServices.map {
                RequestOptionResponse(
                    it.id.toString(),
                    if (it.code == GeneralHelpServiceCode) OtherServiceLabel else it.name,
                    it.shortDescription
                )
            }
        )
        RequestSessionStatus.COLLECTING_ANSWERS -> checkNotNull(session.currentQuestion).toStep()
        RequestSessionStatus.READY_FOR_REVIEW,
        RequestSessionStatus.CONFIRMED -> RequestStepResponse(
            type = RequestStepType.SUMMARY,
            title = if (session.status == RequestSessionStatus.CONFIRMED) {
                "Request published"
            } else {
                "Review your request"
            }
        )
    }

    private fun answerLabel(question: ServiceQuestion, value: String): String =
        question.options.firstOrNull { it.value == value }?.label ?: value

    private fun saveAnswerTranscript(
        session: RequestSession,
        question: ServiceQuestion,
        normalizedValue: String
    ) {
        if (messages.findBySessionIdAndQuestionKeyAndSender(
                session.id,
                question.key,
                MessageSender.ASSISTANT
            ).isEmpty
        ) {
            addMessage(session, MessageSender.ASSISTANT, question.prompt, question.key)
        }
        val displayedAnswer = answerLabel(question, normalizedValue)
        val existingUserMessage = messages.findBySessionIdAndQuestionKeyAndSender(
            session.id,
            question.key,
            MessageSender.USER
        )
        if (existingUserMessage.isPresent) {
            existingUserMessage.get().content = displayedAnswer
            messages.save(existingUserMessage.get())
        } else {
            addMessage(session, MessageSender.USER, displayedAnswer, question.key)
        }
    }

    private fun classifyWithAi(
        description: String,
        categoryCode: String,
        availableServices: List<MarketplaceService>
    ): MarketplaceService? {
        val candidates = availableServices.map { RequirementCandidate(it.code, it.name) }
        val analysis = runCatching {
            aiRequirementService.analyze(RequirementAnalysisRequest(description, categoryCode, candidates))
        }.onFailure {
            logger.warn("AI requirement analysis failed; falling back to service selection", it)
        }.getOrNull() ?: return null
        val validatedCode = aiValidator.validatedServiceCode(analysis, candidates) ?: return null
        return availableServices.firstOrNull { it.code == validatedCode }
    }

    private fun findService(categoryId: UUID, serviceId: UUID): MarketplaceService {
        val service = services.findById(serviceId).orElseThrow {
            notFound("Marketplace service was not found", "SERVICE_NOT_FOUND")
        }
        if (!service.active || service.category.id != categoryId) {
            throw notFound("Marketplace service was not found", "SERVICE_NOT_FOUND")
        }
        return service
    }

    private fun findSelectableService(session: RequestSession, serviceId: UUID): MarketplaceService {
        val service = services.findById(serviceId).orElseThrow {
            notFound("Marketplace service was not found", "SERVICE_NOT_FOUND")
        }
        val isGeneralHelpFallback = session.status == RequestSessionStatus.NEEDS_SERVICE &&
            service.code == GeneralHelpServiceCode
        if (!service.active || (service.category.id != session.category.id && !isGeneralHelpFallback)) {
            throw notFound("Marketplace service was not found", "SERVICE_NOT_FOUND")
        }
        return service
    }

    private fun selectableServices(
        session: RequestSession,
        sessionSuggestions: List<RequestServiceSuggestion>
    ): List<MarketplaceService> {
        val persistedSuggestions = sessionSuggestions.map(RequestServiceSuggestion::service)
        val categoryServices = persistedSuggestions.ifEmpty {
            services.findAllByCategoryIdAndActiveTrueOrderByNameAsc(session.category.id)
        }
        if (categoryServices.any { it.code == GeneralHelpServiceCode }) return categoryServices
        val fallback = services.findByCodeAndActiveTrue(GeneralHelpServiceCode).orElse(null)
        return if (fallback == null) categoryServices else categoryServices + fallback
    }

    private fun rankedSuggestions(
        description: String,
        categoryCode: String,
        candidates: List<MarketplaceService>,
        deterministic: List<RankedServiceMatch>
    ): List<SuggestedService> {
        val ranked = deterministic.take(MaxSuggestions).map {
            SuggestedService(it.service, it.score, it.confidence, MatchSource.DETERMINISTIC, it.matchedPhrase)
        }.toMutableList()
        if (deterministic.firstOrNull()?.confidence != MatchConfidence.LOW && ranked.isNotEmpty()) {
            return ranked
        }
        val aiService = classifyWithAi(description, categoryCode, candidates) ?: return ranked
        ranked.removeAll { it.service.id == aiService.id }
        ranked.add(
            0,
            SuggestedService(aiService, AiSuggestionScore, MatchConfidence.MEDIUM, MatchSource.AI, null)
        )
        return ranked.take(MaxSuggestions)
    }

    private fun persistSuggestions(
        session: RequestSession,
        ranked: List<SuggestedService>,
        selectedServiceId: UUID?
    ) {
        ranked.forEachIndexed { index, match ->
            suggestions.save(
                RequestServiceSuggestion(
                    session = session,
                    service = match.service,
                    score = match.score,
                    confidence = match.confidence,
                    source = match.source,
                    matchedPhrase = match.matchedPhrase,
                    rank = index + 1,
                    selected = match.service.id == selectedServiceId
                )
            )
        }
    }

    private fun seedInitialDescription(
        session: RequestSession,
        service: MarketplaceService
    ): Map<String, String> {
        val description = session.initialDescription ?: return emptyMap()
        val taskQuestion = questionEngine.allQuestions(service.id)
            .firstOrNull { it.key == TaskDetailsQuestionKey } ?: return emptyMap()
        if (answers.findBySessionIdAndQuestionId(session.id, taskQuestion.id).isEmpty) {
            answers.save(RequestAnswer(session = session, question = taskQuestion, value = description))
        }
        messages.findAllBySessionIdOrderBySequenceNumberAsc(session.id)
            .lastOrNull {
                it.sender == MessageSender.USER &&
                    it.questionKey == null &&
                    it.content == description
            }
            ?.let { message ->
                message.questionKey = taskQuestion.key
                messages.save(message)
            }
        return mapOf(taskQuestion.key to description)
    }

    private fun addMessage(
        session: RequestSession,
        sender: MessageSender,
        content: String,
        questionKey: String? = null
    ) {
        messages.save(
            RequestMessage(
                session = session,
                sender = sender,
                content = content,
                questionKey = questionKey,
                sequenceNumber = session.nextMessageSequence++
            )
        )
    }

    private fun ServiceQuestion.toStep() = RequestStepResponse(
        type = RequestStepType.QUESTION,
        questionKey = key,
        title = prompt,
        helperText = helperText,
        answerType = type,
        options = options.map { RequestOptionResponse(it.value, it.label, it.description) }
    )

    private fun com.findprofessional.marketplace.category.ServiceCategory.toContext() =
        RequestContextResponse(id, code, name)

    private fun MarketplaceService.toContext() = RequestContextResponse(id, code, name)

    private fun RequestServiceSuggestion.toResponse() = RequestMatchResponse(
        serviceCode = service.code,
        serviceName = service.name,
        confidence = confidence,
        source = source,
        score = score,
        matchedPhrase = matchedPhrase
    )

    private fun notFound(message: String, code: String) =
        RequestException(message, code, HttpStatus.NOT_FOUND)

    private companion object {
        val logger = LoggerFactory.getLogger(RequestSessionService::class.java)
        const val InitialDescriptionKey = "initial_description"
        const val ServiceSelectionKey = "service_selection"
        const val GeneralHelpServiceCode = "GENERAL_HELP"
        const val OtherServiceLabel = "Other / Not listed"
        const val TaskDetailsQuestionKey = "task_details"
        const val MaxSuggestions = 3
        const val AiSuggestionScore = 30
    }

    private data class SuggestedService(
        val service: MarketplaceService,
        val score: Int,
        val confidence: MatchConfidence,
        val source: MatchSource,
        val matchedPhrase: String?
    )
}
