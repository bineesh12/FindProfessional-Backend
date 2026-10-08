package com.findprofessional.marketplace.professional

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ProfessionalVerificationService(
    private val authorization: ProfessionalAuthorizationService,
    private val profiles: ProfessionalProfileRepository,
    private val verifications: ProfessionalVerificationRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun get(userId: UUID): ProfessionalVerificationResponse {
        authorization.requireProfessional(userId)
        return verifications.findById(userId).map { it.toResponse() }
            .orElse(ProfessionalVerificationResponse(ProfessionalVerificationStatus.NOT_STARTED))
    }

    @Transactional
    fun submit(userId: UUID, input: SubmitProfessionalVerificationRequest): ProfessionalVerificationResponse {
        authorization.requireProfessional(userId)
        if (!profiles.existsById(userId)) {
            throw ProfessionalProfileException(
                "Complete the professional profile before verification",
                "PROFESSIONAL_PROFILE_REQUIRED",
                HttpStatus.CONFLICT
            )
        }
        val countryCode = input.countryCode.trim().uppercase()
        val organizationNumber = input.organizationNumber.trim().uppercase().replace(Whitespace, "")
        if (!OrganizationNumber.matches(organizationNumber) || !input.fTaxConfirmed) {
            throw ProfessionalProfileException(
                "Business verification details are invalid",
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST
            )
        }
        val now = Instant.now(clock)
        val verification = verifications.findById(userId).orElseGet {
            ProfessionalVerification(
                professionalUserId = userId,
                businessType = input.businessType,
                countryCode = countryCode,
                organizationNumber = organizationNumber,
                fTaxConfirmed = true,
                submittedAt = now
            )
        }
        verification.businessType = input.businessType
        verification.countryCode = countryCode
        verification.organizationNumber = organizationNumber
        verification.fTaxConfirmed = true
        verification.status = ProfessionalVerificationStatus.PENDING
        verification.reviewNote = null
        verification.reviewedAt = null
        verification.submittedAt = now
        return verifications.save(verification).toResponse()
    }

    @Transactional(readOnly = true)
    fun requireOfferSubmissionAllowed(userId: UUID) {
        val status = verifications.findById(userId).map(ProfessionalVerification::status).orElse(null)
        if (status !in OfferSubmissionStatuses) {
            throw ProfessionalProfileException(
                "Submit business verification details before sending your first offer",
                "PROFESSIONAL_VERIFICATION_REQUIRED",
                HttpStatus.CONFLICT
            )
        }
    }

    private fun ProfessionalVerification.toResponse() = ProfessionalVerificationResponse(
        status = status,
        businessType = businessType,
        countryCode = countryCode,
        organizationNumber = organizationNumber,
        fTaxConfirmed = fTaxConfirmed,
        reviewNote = reviewNote,
        submittedAt = submittedAt,
        reviewedAt = reviewedAt
    )

    private companion object {
        val Whitespace = Regex("\\s+")
        val OrganizationNumber = Regex("^[A-Z0-9][A-Z0-9-]{4,31}$")
        val OfferSubmissionStatuses = setOf(
            ProfessionalVerificationStatus.PENDING,
            ProfessionalVerificationStatus.VERIFIED
        )
    }
}
