package com.findprofessional.marketplace.subscription

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class ProfessionalSubscriptionControllerTest {
    private val service = mock(ProfessionalSubscriptionService::class.java)
    private val controller = ProfessionalSubscriptionController(service)
    private val adminController = ProfessionalSubscriptionAdminController(service)

    @Test
    fun `status uses authenticated professional`() {
        val userId = UUID.randomUUID()
        val expected = subscriptionStatus()
        `when`(service.getStatus(userId)).thenReturn(expected)

        assertEquals(expected, controller.getStatus(jwt(userId)))
        verify(service).getStatus(userId)
    }

    @Test
    fun `google play verification uses authenticated professional`() {
        val userId = UUID.randomUUID()
        val request = VerifyGooglePlaySubscriptionRequest("purchase-token")
        val expected = subscriptionStatus().copy(plan = ProfessionalPlan.PRO)
        `when`(service.verifyGooglePlayPurchase(userId, request)).thenReturn(expected)

        assertEquals(expected, controller.verifyGooglePlay(jwt(userId), request))
        verify(service).verifyGooglePlayPurchase(userId, request)
    }

    @Test
    fun `app store verification uses authenticated professional`() {
        val userId = UUID.randomUUID()
        val request = VerifyAppStoreSubscriptionRequest("receipt-data")
        val expected = subscriptionStatus().copy(
            plan = ProfessionalPlan.PRO,
            source = SubscriptionSource.APP_STORE
        )
        `when`(service.verifyAppStorePurchase(userId, request)).thenReturn(expected)

        assertEquals(expected, controller.verifyAppStore(jwt(userId), request))
        verify(service).verifyAppStorePurchase(userId, request)
    }

    @Test
    fun `promotional grant uses authenticated administrator`() {
        val adminId = UUID.randomUUID()
        val request = GrantPromotionalSubscriptionRequest(
            professionalUserId = UUID.randomUUID(),
            expiresAt = Instant.parse("2027-01-01T00:00:00Z")
        )
        val expected = subscriptionStatus().copy(
            plan = ProfessionalPlan.PRO,
            source = SubscriptionSource.PROMOTIONAL
        )
        `when`(service.grantPromotionalAccess(adminId, request)).thenReturn(expected)

        assertEquals(expected, adminController.grantPromotionalAccess(jwt(adminId), request))
        verify(service).grantPromotionalAccess(adminId, request)
    }
}

private fun subscriptionStatus() = ProfessionalSubscriptionStatusResponse(
    enabled = true,
    plan = ProfessionalPlan.FREE,
    source = null,
    expiresAt = null,
    freeOpportunityLimit = 5,
    opportunitiesViewed = 0,
    opportunitiesRemaining = 5,
    periodEndsAt = Instant.parse("2026-11-01T00:00:00Z"),
    canViewNewOpportunity = true
)

private fun jwt(userId: UUID) = Jwt.withTokenValue("access-token")
    .header("alg", "HS256")
    .subject(userId.toString())
    .build()
