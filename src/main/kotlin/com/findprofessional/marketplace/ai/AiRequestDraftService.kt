package com.findprofessional.marketplace.ai

data class RequestDraftAnswer(
    val question: String,
    val answer: String
)

data class RequestDraftInput(
    val categoryName: String,
    val serviceName: String,
    val customerDescription: String?,
    val answers: List<RequestDraftAnswer>
)

data class GeneratedRequestDraft(
    val title: String,
    val description: String
)

interface AiRequestDraftService {
    fun generateDraft(input: RequestDraftInput): GeneratedRequestDraft?
}
