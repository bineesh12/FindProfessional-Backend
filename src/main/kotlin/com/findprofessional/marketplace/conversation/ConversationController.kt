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

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val service: ConversationService
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
}
