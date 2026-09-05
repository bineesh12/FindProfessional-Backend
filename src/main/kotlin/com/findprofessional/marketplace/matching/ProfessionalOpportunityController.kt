package com.findprofessional.marketplace.matching

import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/professional/opportunities")
class ProfessionalOpportunityController(
    private val service: ProfessionalOpportunityService,
    private val actions: ProfessionalOpportunityActionService,
    private val attachments: ProfessionalOfferAttachmentService
) {
    @GetMapping
    fun getOpportunities(@AuthenticationPrincipal jwt: Jwt): ProfessionalOpportunitiesResponse =
        service.getOpportunities(UUID.fromString(jwt.subject))

    @GetMapping("/offers")
    fun getOffers(@AuthenticationPrincipal jwt: Jwt): ProfessionalOpportunitiesResponse =
        service.getOffers(UUID.fromString(jwt.subject))

    @GetMapping("/{requestId}")
    fun getOpportunity(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID
    ): ProfessionalOpportunityResponse = service.getOpportunity(UUID.fromString(jwt.subject), requestId)

    @PostMapping("/{requestId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun decline(@AuthenticationPrincipal jwt: Jwt, @PathVariable requestId: UUID) {
        actions.decline(UUID.fromString(jwt.subject), requestId)
    }

    @PutMapping("/{requestId}/offer")
    fun submitOffer(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestBody request: SaveProfessionalOfferRequest
    ): ProfessionalOfferResponse = actions.submitOffer(UUID.fromString(jwt.subject), requestId, request)

    @PutMapping("/{requestId}/offer/draft")
    fun saveOfferDraft(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestBody request: SaveProfessionalOfferRequest
    ): ProfessionalOfferResponse = actions.saveDraft(UUID.fromString(jwt.subject), requestId, request)

    @GetMapping("/{requestId}/offer")
    fun getOffer(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID
    ): ProfessionalOfferResponse = actions.getOffer(UUID.fromString(jwt.subject), requestId)

    @PostMapping("/{requestId}/offer/attachments", consumes = ["multipart/form-data"])
    fun uploadOfferAttachment(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ProfessionalOfferAttachmentResponse = attachments.upload(UUID.fromString(jwt.subject), requestId, file)

    @DeleteMapping("/{requestId}/offer/attachments/{attachmentId}")
    fun deleteOfferAttachment(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @PathVariable attachmentId: UUID
    ) = attachments.delete(UUID.fromString(jwt.subject), requestId, attachmentId)
}
