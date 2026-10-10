package com.findprofessional.marketplace.professional

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class SubmitProfessionalVerificationRequest(
    val businessType: ProfessionalBusinessType,

    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z]{2}$")
    val countryCode: String = "SE",

    @field:NotBlank
    @field:Size(min = 5, max = 32)
    val organizationNumber: String,

    val fTaxConfirmed: Boolean
)

data class ProfessionalVerificationResponse(
    val status: ProfessionalVerificationStatus,
    val businessType: ProfessionalBusinessType? = null,
    val countryCode: String? = null,
    val organizationNumber: String? = null,
    val fTaxConfirmed: Boolean = false,
    val reviewNote: String? = null,
    val submittedAt: Instant? = null,
    val reviewedAt: Instant? = null
)
