package com.findprofessional.marketplace.ai

import org.springframework.stereotype.Component

@Component
class AiResponseValidator {
    fun validatedServiceCode(
        analysis: RequirementAnalysis,
        allowedServices: List<RequirementCandidate>
    ): String? {
        val serviceCode = analysis.serviceCode ?: return null
        val confidence = analysis.confidence ?: return null
        if (confidence !in MinimumConfidence..1.0) return null
        return serviceCode.takeIf { code -> allowedServices.any { it.serviceCode == code } }
    }

    private companion object {
        const val MinimumConfidence = 0.7
    }
}
