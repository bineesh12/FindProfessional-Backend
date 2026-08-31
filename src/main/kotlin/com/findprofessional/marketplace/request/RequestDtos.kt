package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.question.QuestionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class StartRequestSessionRequest(
    val categoryId: UUID,
    val serviceId: UUID? = null
)

data class SubmitDescriptionRequest(
    @field:NotBlank
    @field:Size(min = 10, max = 2000)
    val description: String
)

data class SelectRequestServiceRequest(val serviceId: UUID)

data class AnswerQuestionRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val value: String
)

enum class RequestStepType { DESCRIPTION, SERVICE_SELECTION, QUESTION, SUMMARY }

data class RequestContextResponse(
    val id: UUID,
    val code: String,
    val name: String
)

data class RequestMessageResponse(
    val sender: MessageSender,
    val content: String,
    val questionKey: String? = null,
    val answerValue: String? = null,
    val editable: Boolean = false
)

data class RequestOptionResponse(
    val value: String,
    val label: String,
    val description: String? = null
)

data class RequestStepResponse(
    val type: RequestStepType,
    val questionKey: String? = null,
    val title: String,
    val helperText: String? = null,
    val answerType: QuestionType? = null,
    val options: List<RequestOptionResponse> = emptyList()
)

data class RequestMatchResponse(
    val serviceCode: String,
    val serviceName: String,
    val confidence: MatchConfidence,
    val source: MatchSource,
    val score: Int,
    val matchedPhrase: String? = null
)

data class RequestSessionResponse(
    val id: UUID,
    val status: RequestSessionStatus,
    val category: RequestContextResponse,
    val service: RequestContextResponse?,
    val messages: List<RequestMessageResponse>,
    val currentStep: RequestStepResponse,
    val match: RequestMatchResponse? = null,
    val version: Long
)

data class RequestSummaryDetailResponse(
    val key: String,
    val label: String,
    val value: String
)

data class RequestSummaryResponse(
    val sessionId: UUID,
    val title: String,
    val description: String,
    val categoryName: String,
    val serviceName: String,
    val details: List<RequestSummaryDetailResponse>
)

data class UpdateRequestDraftRequest(
    @field:NotBlank
    @field:Size(min = 5, max = 180)
    val title: String,

    @field:NotBlank
    @field:Size(min = 20, max = 4000)
    val description: String
)

data class ConfirmRequestResponse(
    val requestId: UUID,
    val status: CustomerRequestStatus
)
