package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.ai.AiRequestDraftService
import com.findprofessional.marketplace.ai.RequestDraftAnswer
import com.findprofessional.marketplace.ai.RequestDraftInput
import com.findprofessional.marketplace.question.QuestionEngine
import com.findprofessional.marketplace.question.displayAnswer
import com.findprofessional.marketplace.matching.OpportunityNotificationMatcher
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RequestSummaryService(
    private val sessionService: RequestSessionService,
    private val answers: RequestAnswerRepository,
    private val requests: CustomerRequestRepository,
    private val sessions: RequestSessionRepository,
    private val questionEngine: QuestionEngine,
    private val aiDraftService: AiRequestDraftService,
    private val locationService: RequestLocationService,
    private val opportunityMatcher: OpportunityNotificationMatcher,
    private val notifications: NotificationService
) {
    @Transactional
    fun summary(userId: UUID, sessionId: UUID): RequestSummaryResponse {
        val session = sessionService.ownedSession(userId, sessionId)
        if (session.status !in setOf(RequestSessionStatus.READY_FOR_REVIEW, RequestSessionStatus.CONFIRMED)) {
            throw RequestException("Complete the questions before reviewing the request", "REQUEST_NOT_READY")
        }
        return buildSummary(session)
    }

    @Transactional
    fun updateDraft(
        userId: UUID,
        sessionId: UUID,
        request: UpdateRequestDraftRequest
    ): RequestSummaryResponse {
        val session = sessionService.ownedSession(userId, sessionId)
        if (session.status != RequestSessionStatus.READY_FOR_REVIEW) {
            throw RequestException("The request is not ready for editing", "REQUEST_NOT_READY")
        }
        session.draftTitle = request.title.trim()
        session.draftDescription = request.description.trim()
        session.touch()
        sessions.save(session)
        return buildSummary(session)
    }

    @Transactional
    fun confirm(userId: UUID, sessionId: UUID): ConfirmRequestResponse {
        val session = sessionService.ownedSession(userId, sessionId)
        val existing = requests.findBySessionId(session.id).orElse(null)
        if (existing != null) {
            return ConfirmRequestResponse(existing.id, existing.status)
        }
        if (session.status != RequestSessionStatus.READY_FOR_REVIEW) {
            throw RequestException("The request is not ready to publish", "REQUEST_NOT_READY")
        }

        val service = checkNotNull(session.service)
        val summary = buildSummary(session)
        val request = requests.save(
            CustomerRequest(
                session = session,
                customerId = userId,
                category = session.category,
                service = service,
                title = summary.title,
                description = summary.description
            )
        )
        val answerMap = answers.findAllBySessionId(session.id).associate { it.question.key to it.value }
        locationService.saveResolvedLocations(request, answerMap)
        opportunityMatcher.matchingProfessionalIds(request).forEach { professionalId ->
            notifications.create(
                CreateNotification(
                    userId = professionalId,
                    type = MarketplaceNotificationType.NEW_OPPORTUNITY,
                    title = "New opportunity",
                    body = "A new ${service.name} request matches your services",
                    requestId = request.id
                )
            )
        }
        session.status = RequestSessionStatus.CONFIRMED
        session.currentQuestion = null
        session.touch()
        sessions.save(session)
        return ConfirmRequestResponse(request.id, request.status)
    }

    private fun buildSummary(session: RequestSession): RequestSummaryResponse {
        val service = checkNotNull(session.service)
        val answerMap = answers.findAllBySessionId(session.id).associateBy { it.question.key }
        val details = questionEngine.allQuestions(service.id).mapNotNull { question ->
            val answer = answerMap[question.key] ?: return@mapNotNull null
            RequestSummaryDetailResponse(
                key = question.key,
                label = question.prompt,
                value = question.displayAnswer(answer.value)
            )
        }
        ensureDraft(session, details)
        return RequestSummaryResponse(
            sessionId = session.id,
            title = checkNotNull(session.draftTitle),
            description = checkNotNull(session.draftDescription),
            categoryName = session.category.name,
            serviceName = service.name,
            details = details
        )
    }

    private fun ensureDraft(
        session: RequestSession,
        details: List<RequestSummaryDetailResponse>
    ) {
        if (!session.draftTitle.isNullOrBlank() && !session.draftDescription.isNullOrBlank()) return
        val service = checkNotNull(session.service)
        val fallbackDescription = deterministicDescription(session, details)
        val generated = runCatching {
            aiDraftService.generateDraft(
                RequestDraftInput(
                    categoryName = session.category.name,
                    serviceName = service.name,
                    customerDescription = session.initialDescription,
                    answers = details.map { RequestDraftAnswer(it.label, it.value) }
                )
            )
        }.onFailure {
            logger.warn("AI request draft generation failed; using deterministic draft", it)
        }.getOrNull()?.takeIf {
            it.title.trim().length in 5..180 && it.description.trim().length in 20..4000
        }
        session.draftTitle = generated?.title?.trim() ?: service.name
        session.draftDescription = generated?.description?.trim() ?: fallbackDescription
        session.touch()
        sessions.save(session)
    }

    private fun deterministicDescription(
        session: RequestSession,
        details: List<RequestSummaryDetailResponse>
    ): String = buildString {
        session.initialDescription
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let(::append)
        if (details.isNotEmpty()) {
            if (isNotEmpty()) append("\n\n")
            append("Project details:\n")
            details.forEachIndexed { index, detail ->
                append("- ")
                append(detail.label.trim().removeSuffix("?"))
                append(": ")
                append(detail.value.trim())
                if (index != details.lastIndex) append('\n')
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(RequestSummaryService::class.java)
    }
}
