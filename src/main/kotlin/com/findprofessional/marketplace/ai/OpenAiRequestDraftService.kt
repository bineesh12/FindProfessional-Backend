package com.findprofessional.marketplace.ai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class OpenAiRequestDraftService(
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper,
    private val properties: OpenAiProperties
) : AiRequestDraftService {
    private val client = restClientBuilder
        .baseUrl(properties.baseUrl.trimEnd('/'))
        .build()

    override fun generateDraft(input: RequestDraftInput): GeneratedRequestDraft? {
        if (properties.apiKey.isBlank()) return null
        val response = client.post()
            .uri("/responses")
            .contentType(MediaType.APPLICATION_JSON)
            .headers { it.setBearerAuth(properties.apiKey) }
            .body(requestBody(input))
            .retrieve()
            .body(JsonNode::class.java)
            ?: return null
        val outputText = response.path("output")
            .flatMap { it.path("content").toList() }
            .firstOrNull { it.path("type").asText() == "output_text" }
            ?.path("text")
            ?.asText()
            ?: response.path("output_text").asText().takeIf(String::isNotBlank)
            ?: return null
        return runCatching {
            objectMapper.readValue(outputText, GeneratedRequestDraft::class.java)
        }.getOrNull()
    }

    private fun requestBody(input: RequestDraftInput): Map<String, Any> = mapOf(
        "model" to properties.model,
        "store" to false,
        "reasoning" to mapOf("effort" to "none"),
        "max_output_tokens" to properties.maxOutputTokens,
        "instructions" to RequestDraftPrompt.Instructions.trimIndent(),
        "input" to objectMapper.writeValueAsString(input),
        "text" to mapOf(
            "verbosity" to "low",
            "format" to mapOf(
                "type" to "json_schema",
                "name" to "marketplace_request_draft",
                "strict" to true,
                "schema" to mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "properties" to mapOf(
                        "title" to mapOf(
                            "type" to "string",
                            "description" to "A specific request title between 5 and 180 characters."
                        ),
                        "description" to mapOf(
                            "type" to "string",
                            "description" to "A factual request description between 20 and 4000 characters."
                        )
                    ),
                    "required" to listOf("title", "description")
                )
            )
        )
    )
}
