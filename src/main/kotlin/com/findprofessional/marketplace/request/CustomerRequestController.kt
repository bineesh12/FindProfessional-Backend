package com.findprofessional.marketplace.request

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.http.HttpStatus
import java.util.UUID

@RestController
@RequestMapping("/api/customer/requests")
class CustomerRequestController(
    private val service: CustomerRequestQueryService,
    private val offerDecisions: CustomerOfferDecisionService,
    private val cancellations: CustomerRequestCancellationService
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "ALL") status: CustomerRequestFilter,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): CustomerRequestListResponse = service.list(UUID.fromString(jwt.subject), status, page, size)

    @GetMapping("/{requestId}/offers")
    fun offers(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestParam(defaultValue = "LATEST") sort: CustomerOfferSort
    ): CustomerRequestOffersResponse = service.offers(UUID.fromString(jwt.subject), requestId, sort)

    @GetMapping("/{requestId}/offers/{offerId}")
    fun offerDetails(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @PathVariable offerId: UUID
    ): CustomerOfferDetailsResponse = service.offerDetails(UUID.fromString(jwt.subject), requestId, offerId)

    @PostMapping("/{requestId}/offers/{offerId}/accept")
    fun acceptOffer(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @PathVariable offerId: UUID
    ): CustomerOfferDecisionResponse =
        offerDecisions.accept(UUID.fromString(jwt.subject), requestId, offerId)

    @PostMapping("/{requestId}/offers/{offerId}/decline")
    fun declineOffer(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @PathVariable offerId: UUID
    ): CustomerOfferDecisionResponse =
        offerDecisions.decline(UUID.fromString(jwt.subject), requestId, offerId)

    @PutMapping("/{requestId}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestBody input: UpdateCustomerRequestRequest
    ): CustomerRequestUpdateResponse = service.update(UUID.fromString(jwt.subject), requestId, input)

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID
    ) = cancellations.cancel(UUID.fromString(jwt.subject), requestId)
}
