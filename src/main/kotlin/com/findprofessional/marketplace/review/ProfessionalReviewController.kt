package com.findprofessional.marketplace.review

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/customer/requests/{requestId}/review")
class CustomerReviewController(private val reviews: ProfessionalReviewService) {
    @GetMapping
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID
    ): CustomerReviewStateResponse = reviews.customerReview(UUID.fromString(jwt.subject), requestId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: SaveProfessionalReviewRequest
    ): ProfessionalReviewResponse = reviews.create(UUID.fromString(jwt.subject), requestId, request)

    @PutMapping
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: SaveProfessionalReviewRequest
    ): ProfessionalReviewResponse = reviews.update(UUID.fromString(jwt.subject), requestId, request)
}

@RestController
@RequestMapping("/api/professional-profile/reviews")
class ProfessionalReviewsController(private val reviews: ProfessionalReviewService) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ProfessionalReviewsResponse = reviews.professionalReviews(UUID.fromString(jwt.subject), page, size)
}
