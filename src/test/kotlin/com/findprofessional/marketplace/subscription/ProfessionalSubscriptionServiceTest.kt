package com.findprofessional.marketplace.subscription

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.RequestSession
import com.findprofessional.marketplace.request.RequestSessionStatus
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.any
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class ProfessionalSubscriptionServiceTest {
    private val authorization = mock(ProfessionalAuthorizationService::class.java)
    private val users = mock(UserAccountRepository::class.java)
    private val entitlements = mock(ProfessionalSubscriptionEntitlementRepository::class.java)
    private val views = mock(ProfessionalOpportunityViewRepository::class.java)
    private val verifier = mock(GooglePlaySubscriptionVerifier::class.java)
    private val appStoreVerifier = mock(AppStoreSubscriptionVerifier::class.java)
    private val now = Instant.parse("2026-10-07T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val professionalId = UUID.randomUUID()

    @Test
    fun `free status reports remaining views for current calendar month`() {
        val service = service(freeLimit = 5)
        `when`(entitlements.findFirstByProfessionalUserIdAndRevokedAtIsNullAndStartsAtLessThanEqualAndExpiresAtAfterOrderByExpiresAtDesc(
            professionalId, now, now
        )).thenReturn(Optional.empty())
        `when`(views.countByProfessionalUserIdAndViewedAtGreaterThanEqualAndViewedAtLessThan(
            professionalId,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-11-01T00:00:00Z")
        )).thenReturn(3)

        val status = service.getStatus(professionalId)

        assertEquals(ProfessionalPlan.FREE, status.plan)
        assertEquals(2, status.opportunitiesRemaining)
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), status.periodEndsAt)
    }

    @Test
    fun `previously viewed opportunity remains available after allowance is used`() {
        val service = service(freeLimit = 1)
        val request = request()
        `when`(views.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(true)

        service.registerOpportunityView(professionalId, request)

        verify(views, never()).save(any(ProfessionalOpportunityView::class.java))
    }

    @Test
    fun `new opportunity is rejected when free allowance is used`() {
        val service = service(freeLimit = 1)
        val request = request()
        `when`(views.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(false)
        `when`(users.findLockedById(professionalId)).thenReturn(Optional.of(UserAccount(id = professionalId, displayName = "Pro")))
        `when`(entitlements.findFirstByProfessionalUserIdAndRevokedAtIsNullAndStartsAtLessThanEqualAndExpiresAtAfterOrderByExpiresAtDesc(
            professionalId, now, now
        )).thenReturn(Optional.empty())
        `when`(views.countByProfessionalUserIdAndViewedAtGreaterThanEqualAndViewedAtLessThan(
            professionalId,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-11-01T00:00:00Z")
        )).thenReturn(1)

        val error = assertThrows(SubscriptionException::class.java) {
            service.registerOpportunityView(professionalId, request)
        }

        assertEquals("SUBSCRIPTION_REQUIRED", error.code)
        verify(views, never()).save(any(ProfessionalOpportunityView::class.java))
    }

    @Test
    fun `active promotional entitlement grants pro access until expiry`() {
        val adminId = UUID.randomUUID()
        val expiry = now.plusSeconds(90L * 24 * 60 * 60)
        val service = service(adminIds = setOf(adminId))
        `when`(entitlements.save(any(ProfessionalSubscriptionEntitlement::class.java))).thenAnswer { it.arguments[0] }
        `when`(entitlements.findFirstByProfessionalUserIdAndRevokedAtIsNullAndStartsAtLessThanEqualAndExpiresAtAfterOrderByExpiresAtDesc(
            professionalId, now, now
        )).thenAnswer {
            Optional.of(
                ProfessionalSubscriptionEntitlement(
                    professionalUserId = professionalId,
                    source = SubscriptionSource.PROMOTIONAL,
                    startsAt = now,
                    expiresAt = expiry,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        `when`(views.countByProfessionalUserIdAndViewedAtGreaterThanEqualAndViewedAtLessThan(
            professionalId,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-11-01T00:00:00Z")
        )).thenReturn(5)

        val status = service.grantPromotionalAccess(
            adminId,
            GrantPromotionalSubscriptionRequest(professionalId, expiry, "Launch promotion")
        )

        assertEquals(ProfessionalPlan.PRO, status.plan)
        assertEquals(SubscriptionSource.PROMOTIONAL, status.source)
        assertEquals(null, status.opportunitiesRemaining)
    }

    @Test
    fun `verified app store receipt grants pro access to its owner`() {
        val expiry = now.plusSeconds(30L * 24 * 60 * 60)
        val service = service()
        `when`(appStoreVerifier.verify("receipt-data")).thenReturn(
            VerifiedAppStoreSubscription("arbio_pro_monthly", "original-transaction", expiry)
        )
        `when`(entitlements.findBySourceAndExternalReference(
            SubscriptionSource.APP_STORE,
            "ac456167e424e0edc71e50f914658dd1554cb2ad03efb28278ca5a1778fb109c"
        )).thenReturn(Optional.empty())
        `when`(entitlements.save(any(ProfessionalSubscriptionEntitlement::class.java))).thenAnswer { it.arguments[0] }
        `when`(entitlements.findFirstByProfessionalUserIdAndRevokedAtIsNullAndStartsAtLessThanEqualAndExpiresAtAfterOrderByExpiresAtDesc(
            professionalId, now, now
        )).thenAnswer {
            Optional.of(
                ProfessionalSubscriptionEntitlement(
                    professionalUserId = professionalId,
                    source = SubscriptionSource.APP_STORE,
                    startsAt = now,
                    expiresAt = expiry,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        `when`(views.countByProfessionalUserIdAndViewedAtGreaterThanEqualAndViewedAtLessThan(
            professionalId,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-11-01T00:00:00Z")
        )).thenReturn(5)

        val status = service.verifyAppStorePurchase(
            professionalId,
            VerifyAppStoreSubscriptionRequest("receipt-data")
        )

        assertEquals(ProfessionalPlan.PRO, status.plan)
        assertEquals(SubscriptionSource.APP_STORE, status.source)
        assertEquals(null, status.opportunitiesRemaining)
    }

    private fun service(
        freeLimit: Int = 5,
        adminIds: Set<UUID> = emptySet()
    ) = ProfessionalSubscriptionService(
        authorization,
        users,
        entitlements,
        views,
        verifier,
        appStoreVerifier,
        SubscriptionProperties(enabled = true, freeOpportunityLimit = freeLimit, adminUserIds = adminIds),
        clock
    )

    private fun request(): CustomerRequest {
        val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
        val marketplaceService = MarketplaceService(
            category = category,
            code = "ROOFING",
            name = "Roofing",
            shortDescription = "Roof work",
            iconKey = "roofing"
        )
        val session = RequestSession(
            customerId = UUID.randomUUID(),
            category = category,
            service = marketplaceService,
            status = RequestSessionStatus.CONFIRMED
        )
        return CustomerRequest(
            session = session,
            customerId = session.customerId,
            category = category,
            service = marketplaceService,
            title = "Repair roof",
            description = "Repair a leaking roof"
        )
    }
}
