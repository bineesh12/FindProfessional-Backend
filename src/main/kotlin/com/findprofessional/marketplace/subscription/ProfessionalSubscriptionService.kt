package com.findprofessional.marketplace.subscription

import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.user.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class ProfessionalSubscriptionService(
    private val authorization: ProfessionalAuthorizationService,
    private val users: UserAccountRepository,
    private val entitlements: ProfessionalSubscriptionEntitlementRepository,
    private val opportunityViews: ProfessionalOpportunityViewRepository,
    private val googlePlayVerifier: GooglePlaySubscriptionVerifier,
    private val appStoreVerifier: AppStoreSubscriptionVerifier,
    private val properties: SubscriptionProperties,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun getStatus(userId: UUID): ProfessionalSubscriptionStatusResponse {
        authorization.requireProfessional(userId)
        return statusFor(userId, Instant.now(clock))
    }

    @Transactional
    fun registerOpportunityView(userId: UUID, request: CustomerRequest) {
        authorization.requireProfessional(userId)
        if (!properties.enabled || opportunityViews.existsByProfessionalUserIdAndRequestId(userId, request.id)) return

        // Serializing new views per account prevents concurrent requests from exceeding the allowance.
        users.findLockedById(userId).orElseThrow {
            SubscriptionException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (opportunityViews.existsByProfessionalUserIdAndRequestId(userId, request.id)) return

        val now = Instant.now(clock)
        val status = statusFor(userId, now)
        if (!status.canViewNewOpportunity) {
            throw SubscriptionException(
                "The monthly opportunity allowance has been used",
                "SUBSCRIPTION_REQUIRED",
                HttpStatus.PAYMENT_REQUIRED
            )
        }
        opportunityViews.save(
            ProfessionalOpportunityView(
                professionalUserId = userId,
                request = request,
                viewedAt = now
            )
        )
    }

    @Transactional
    fun verifyGooglePlayPurchase(
        userId: UUID,
        request: VerifyGooglePlaySubscriptionRequest
    ): ProfessionalSubscriptionStatusResponse {
        authorization.requireProfessional(userId)
        if (!properties.enabled) {
            throw SubscriptionException(
                "Professional subscriptions are not available",
                "SUBSCRIPTIONS_DISABLED",
                HttpStatus.NOT_FOUND
            )
        }
        val verified = googlePlayVerifier.verify(request.purchaseToken)
        val now = Instant.now(clock)
        val reference = request.purchaseToken.sha256()
        val entitlement = entitlements
            .findBySourceAndExternalReference(SubscriptionSource.GOOGLE_PLAY, reference)
            .orElseGet {
                ProfessionalSubscriptionEntitlement(
                    professionalUserId = userId,
                    source = SubscriptionSource.GOOGLE_PLAY,
                    externalReference = reference,
                    startsAt = now,
                    expiresAt = verified.expiresAt,
                    createdAt = now,
                    updatedAt = now
                )
            }
        if (entitlement.professionalUserId != userId) {
            throw SubscriptionException(
                "This subscription belongs to another account",
                "PURCHASE_ACCOUNT_MISMATCH",
                HttpStatus.CONFLICT
            )
        }
        entitlement.expiresAt = verified.expiresAt
        entitlement.revokedAt = null
        entitlement.updatedAt = now
        entitlements.save(entitlement)
        return statusFor(userId, now)
    }

    @Transactional
    fun verifyAppStorePurchase(
        userId: UUID,
        request: VerifyAppStoreSubscriptionRequest
    ): ProfessionalSubscriptionStatusResponse {
        authorization.requireProfessional(userId)
        requireSubscriptionsEnabled()
        val verified = appStoreVerifier.verify(request.receiptData)
        val now = Instant.now(clock)
        saveStoreEntitlement(
            userId = userId,
            source = SubscriptionSource.APP_STORE,
            externalReference = verified.originalTransactionId.sha256(),
            expiresAt = verified.expiresAt,
            now = now
        )
        return statusFor(userId, now)
    }

    @Transactional
    fun grantPromotionalAccess(
        administratorId: UUID,
        request: GrantPromotionalSubscriptionRequest
    ): ProfessionalSubscriptionStatusResponse {
        if (administratorId !in properties.adminUserIds) {
            throw SubscriptionException("Administrator access is required", "ADMIN_REQUIRED", HttpStatus.FORBIDDEN)
        }
        authorization.requireProfessional(request.professionalUserId)
        val now = Instant.now(clock)
        if (!request.expiresAt.isAfter(now)) {
            throw SubscriptionException(
                "Promotional access must expire in the future",
                "INVALID_PROMOTIONAL_EXPIRY",
                HttpStatus.BAD_REQUEST
            )
        }
        entitlements.save(
            ProfessionalSubscriptionEntitlement(
                professionalUserId = request.professionalUserId,
                source = SubscriptionSource.PROMOTIONAL,
                startsAt = now,
                expiresAt = request.expiresAt,
                note = request.note?.trim()?.takeUnless(String::isBlank),
                createdAt = now,
                updatedAt = now
            )
        )
        return statusFor(request.professionalUserId, now)
    }

    private fun statusFor(userId: UUID, now: Instant): ProfessionalSubscriptionStatusResponse {
        val period = monthlyPeriod(now)
        val entitlement = entitlements
            .findFirstByProfessionalUserIdAndRevokedAtIsNullAndStartsAtLessThanEqualAndExpiresAtAfterOrderByExpiresAtDesc(
                userId,
                now,
                now
            )
            .orElse(null)
        val viewed = opportunityViews.countByProfessionalUserIdAndViewedAtGreaterThanEqualAndViewedAtLessThan(
            userId,
            period.first,
            period.second
        )
        val isPro = entitlement != null
        val remaining = if (isPro) null else (properties.freeOpportunityLimit.toLong() - viewed).coerceAtLeast(0)
        return ProfessionalSubscriptionStatusResponse(
            enabled = properties.enabled,
            plan = if (isPro) ProfessionalPlan.PRO else ProfessionalPlan.FREE,
            source = entitlement?.source,
            expiresAt = entitlement?.expiresAt,
            freeOpportunityLimit = properties.freeOpportunityLimit,
            opportunitiesViewed = viewed,
            opportunitiesRemaining = remaining,
            periodEndsAt = period.second,
            canViewNewOpportunity = !properties.enabled || isPro || checkNotNull(remaining) > 0
        )
    }

    private fun requireSubscriptionsEnabled() {
        if (!properties.enabled) {
            throw SubscriptionException(
                "Professional subscriptions are not available",
                "SUBSCRIPTIONS_DISABLED",
                HttpStatus.NOT_FOUND
            )
        }
    }

    private fun saveStoreEntitlement(
        userId: UUID,
        source: SubscriptionSource,
        externalReference: String,
        expiresAt: Instant,
        now: Instant
    ) {
        val entitlement = entitlements.findBySourceAndExternalReference(source, externalReference).orElseGet {
            ProfessionalSubscriptionEntitlement(
                professionalUserId = userId,
                source = source,
                externalReference = externalReference,
                startsAt = now,
                expiresAt = expiresAt,
                createdAt = now,
                updatedAt = now
            )
        }
        if (entitlement.professionalUserId != userId) {
            throw SubscriptionException(
                "This subscription belongs to another account",
                "PURCHASE_ACCOUNT_MISMATCH",
                HttpStatus.CONFLICT
            )
        }
        entitlement.expiresAt = expiresAt
        entitlement.revokedAt = null
        entitlement.updatedAt = now
        entitlements.save(entitlement)
    }

    private fun monthlyPeriod(now: Instant): Pair<Instant, Instant> {
        val current = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
        val start = current.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay(ZoneOffset.UTC)
        return start.toInstant() to start.plusMonths(1).toInstant()
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
