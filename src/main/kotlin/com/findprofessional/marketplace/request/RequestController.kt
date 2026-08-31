package com.findprofessional.marketplace.request

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/request-sessions")
class RequestController(
    private val sessions: RequestSessionService,
    private val summaries: RequestSummaryService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun start(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: StartRequestSessionRequest
    ): RequestSessionResponse = sessions.start(jwt.userId(), request)

    @GetMapping("/{sessionId}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID
    ): RequestSessionResponse = sessions.get(jwt.userId(), sessionId)

    @PostMapping("/{sessionId}/description")
    fun submitDescription(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @Valid @RequestBody request: SubmitDescriptionRequest
    ): RequestSessionResponse = sessions.submitDescription(jwt.userId(), sessionId, request)

    @PutMapping("/{sessionId}/service")
    fun selectService(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @RequestBody request: SelectRequestServiceRequest
    ): RequestSessionResponse = sessions.selectService(jwt.userId(), sessionId, request)

    @PutMapping("/{sessionId}/answers/{questionKey}")
    fun answer(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @PathVariable questionKey: String,
        @Valid @RequestBody request: AnswerQuestionRequest
    ): RequestSessionResponse = sessions.answer(jwt.userId(), sessionId, questionKey, request)

    @PostMapping("/{sessionId}/answers/{questionKey}/edit")
    fun editAnswer(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @PathVariable questionKey: String
    ): RequestSessionResponse = sessions.editAnswer(jwt.userId(), sessionId, questionKey)

    @GetMapping("/{sessionId}/summary")
    fun summary(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID
    ): RequestSummaryResponse = summaries.summary(jwt.userId(), sessionId)

    @PutMapping("/{sessionId}/draft")
    fun updateDraft(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @Valid @RequestBody request: UpdateRequestDraftRequest
    ): RequestSummaryResponse = summaries.updateDraft(jwt.userId(), sessionId, request)

    @PostMapping("/{sessionId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    fun confirm(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID
    ): ConfirmRequestResponse = summaries.confirm(jwt.userId(), sessionId)

    private fun Jwt.userId() = UUID.fromString(subject)
}
