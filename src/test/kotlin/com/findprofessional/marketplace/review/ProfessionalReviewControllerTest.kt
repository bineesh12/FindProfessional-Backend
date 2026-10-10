package com.findprofessional.marketplace.review

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class ProfessionalReviewControllerTest {
    private val service = mock(ProfessionalReviewService::class.java)

    @Test
    fun `customer create uses authenticated subject and request id`() {
        val controller = CustomerReviewController(service)
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val input = SaveProfessionalReviewRequest(5, "Excellent work")
        val expected = response(requestId)
        `when`(service.create(userId, requestId, input)).thenReturn(expected)

        assertSame(expected, controller.create(jwt(userId), requestId, input))
        verify(service).create(userId, requestId, input)
    }

    @Test
    fun `professional list uses authenticated subject`() {
        val controller = ProfessionalReviewsController(service)
        val userId = UUID.randomUUID()
        val expected = ProfessionalReviewsResponse(
            ProfessionalRatingSummaryResponse(null, 0, (5 downTo 1).map { RatingBreakdownResponse(it, 0) }),
            emptyList(), 0, 20, 0, 0
        )
        `when`(service.professionalReviews(userId, 0, 20)).thenReturn(expected)

        assertSame(expected, controller.list(jwt(userId), 0, 20))
        verify(service).professionalReviews(userId, 0, 20)
    }

    private fun jwt(userId: UUID) = Jwt(
        "token", Instant.now(), Instant.now().plusSeconds(60),
        mapOf("alg" to "none"), mapOf("sub" to userId.toString())
    )

    private fun response(requestId: UUID) = ProfessionalReviewResponse(
        UUID.randomUUID(), requestId, UUID.randomUUID(), UUID.randomUUID(),
        ReviewCustomerResponse("Anna", null), "Roofing", 5, "Excellent work",
        Instant.now(), Instant.now()
    )
}
