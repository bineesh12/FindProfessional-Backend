package com.findprofessional.marketplace.conversation

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import com.findprofessional.marketplace.user.CreateUserReportRequest
import com.findprofessional.marketplace.user.ConversationSafetyResponse
import com.findprofessional.marketplace.user.UserSafetyService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val service: ConversationService,
    private val safety: UserSafetyService
) {
    @PostMapping
    fun open(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody input: OpenConversationRequest
    ): ConversationSummaryResponse = service.open(UUID.fromString(jwt.subject), input)

    @GetMapping
    fun list(@AuthenticationPrincipal jwt: Jwt): List<ConversationSummaryResponse> =
        service.list(UUID.fromString(jwt.subject))

    @GetMapping("/{conversationId}/messages")
    fun messages(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: UUID
    ): ConversationThreadResponse = service.thread(UUID.fromString(jwt.subject), conversationId)

    @PostMapping("/{conversationId}/messages")
    fun send(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: UUID,
        @Valid @RequestBody input: SendConversationMessageRequest
    ): ConversationMessageResponse = service.send(UUID.fromString(jwt.subject), conversationId, input)

    @PostMapping("/{conversationId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun report(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: UUID,
        @Valid @RequestBody input: CreateUserReportRequest
    ) = safety.report(UUID.fromString(jwt.subject), conversationId, input)

    @PostMapping("/{conversationId}/block")
    fun block(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: UUID
    ): ConversationSafetyResponse = safety.block(UUID.fromString(jwt.subject), conversationId)

    @DeleteMapping("/{conversationId}/block")
    fun unblock(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: UUID
    ): ConversationSafetyResponse = safety.unblock(UUID.fromString(jwt.subject), conversationId)
}
