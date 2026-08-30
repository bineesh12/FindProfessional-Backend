package com.findprofessional.marketplace.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AiResponseValidatorTest {
    private val validator = AiResponseValidator()
    private val allowed = listOf(RequirementCandidate("ROOFING", "Roofing"))

    @Test
    fun `accepts only confident service codes from allowed catalog`() {
        assertEquals(
            "ROOFING",
            validator.validatedServiceCode(RequirementAnalysis("ROOFING", 0.9), allowed)
        )
        assertNull(validator.validatedServiceCode(RequirementAnalysis("PLUMBING", 0.9), allowed))
        assertNull(validator.validatedServiceCode(RequirementAnalysis("ROOFING", 0.4), allowed))
    }
}
