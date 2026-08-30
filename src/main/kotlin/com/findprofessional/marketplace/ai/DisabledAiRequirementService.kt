package com.findprofessional.marketplace.ai

import org.springframework.stereotype.Service

@Service
class DisabledAiRequirementService : AiRequirementService {
    override fun analyze(request: RequirementAnalysisRequest) = RequirementAnalysis()
}
