package com.findprofessional.marketplace.professional

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class ProfessionalVerificationServiceTest {
    private val authorization = mock(ProfessionalAuthorizationService::class.java)
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val verifications = mock(ProfessionalVerificationRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC)
    private val service = ProfessionalVerificationService(authorization, profiles, verifications, clock)

    @Test
    fun `missing verification returns not started`() {
        val userId = UUID.randomUUID()
        `when`(verifications.findById(userId)).thenReturn(Optional.empty())

        val response = service.get(userId)

        assertEquals(ProfessionalVerificationStatus.NOT_STARTED, response.status)
    }

    @Test
    fun `submission normalizes business details and becomes pending`() {
        val userId = UUID.randomUUID()
        `when`(profiles.existsById(userId)).thenReturn(true)
        `when`(verifications.findById(userId)).thenReturn(Optional.empty())
        `when`(verifications.save(any(ProfessionalVerification::class.java))).thenAnswer { it.arguments[0] }

        val response = service.submit(
            userId,
            SubmitProfessionalVerificationRequest(
                businessType = ProfessionalBusinessType.COMPANY,
                countryCode = "se",
                organizationNumber = "556 123-4567",
                fTaxConfirmed = true
            )
        )

        assertEquals(ProfessionalVerificationStatus.PENDING, response.status)
        assertEquals("SE", response.countryCode)
        assertEquals("556123-4567", response.organizationNumber)
        assertEquals(Instant.parse("2026-10-07T12:00:00Z"), response.submittedAt)
    }

    @Test
    fun `offer submission requires pending or verified business details`() {
        val userId = UUID.randomUUID()
        `when`(verifications.findById(userId)).thenReturn(Optional.empty())

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.requireOfferSubmissionAllowed(userId)
        }

        assertEquals("PROFESSIONAL_VERIFICATION_REQUIRED", error.code)
    }

    @Test
    fun `pending verification allows offer submission`() {
        val userId = UUID.randomUUID()
        `when`(verifications.findById(userId)).thenReturn(
            Optional.of(
                ProfessionalVerification(
                    professionalUserId = userId,
                    businessType = ProfessionalBusinessType.SOLE_TRADER,
                    countryCode = "SE",
                    organizationNumber = "850101-1234",
                    fTaxConfirmed = true
                )
            )
        )

        service.requireOfferSubmissionAllowed(userId)
    }
}
