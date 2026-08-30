package com.findprofessional.marketplace.ai

class DisabledAiRequestDraftService : AiRequestDraftService {
    override fun generateDraft(input: RequestDraftInput): GeneratedRequestDraft? = null
}
