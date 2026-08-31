package com.findprofessional.marketplace.ai

data class RequirementCandidate(
    val serviceCode: String,
    val serviceName: String
)

data class RequirementAnalysisRequest(
    val description: String,
    val categoryCode: String,
    val allowedServices: List<RequirementCandidate>
)

data class RequirementAnalysis(
    val serviceCode: String? = null,
    val confidence: Double? = null,
    val extractedAnswers: Map<String, String> = emptyMap(),
    val missingInformation: List<String> = emptyList()
)

interface AiRequirementService {
    fun analyze(request: RequirementAnalysisRequest): RequirementAnalysis
}
