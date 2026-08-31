package com.findprofessional.marketplace.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

class OpenAiRequestDraftServiceTest {
    private val objectMapper = jacksonObjectMapper()
    private val input = RequestDraftInput(
        categoryName = "Home",
        serviceName = "Appliance repair",
        customerDescription = "My washing machine leaks",
        answers = listOf(RequestDraftAnswer("Which appliance?", "Washing machine"))
    )

    @Test
    fun `generates typed draft using stateless structured output`() {
        val fixture = fixture()
        val generatedJson = objectMapper.writeValueAsString(
            GeneratedRequestDraft(
                "Repair leaking washing machine",
                "The washing machine leaks during each wash cycle."
            )
        )
        val responseJson = objectMapper.writeValueAsString(
            mapOf(
                "output" to listOf(
                    mapOf(
                        "type" to "message",
                        "content" to listOf(mapOf("type" to "output_text", "text" to generatedJson))
                    )
                )
            )
        )
        fixture.server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(jsonPath("$.store").value(false))
            .andExpect(jsonPath("$.model").value("gpt-5.6-luna"))
            .andExpect(jsonPath("$.text.format.type").value("json_schema"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val draft = fixture.service.generateDraft(input)

        assertEquals("Repair leaking washing machine", draft?.title)
        assertEquals("The washing machine leaks during each wash cycle.", draft?.description)
        fixture.server.verify()
    }

    @Test
    fun `returns null when provider output cannot be parsed`() {
        val fixture = fixture()
        val responseJson = """{"output":[{"content":[{"type":"output_text","text":"not-json"}]}]}"""
        fixture.server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        assertNull(fixture.service.generateDraft(input))
    }

    @Test
    fun `provider errors propagate to deterministic fallback boundary`() {
        val fixture = fixture()
        fixture.server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andRespond(withServerError())

        assertThrows(RestClientResponseException::class.java) {
            fixture.service.generateDraft(input)
        }
    }

    private fun fixture(): Fixture {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = OpenAiRequestDraftService(
            builder,
            objectMapper,
            OpenAiProperties(apiKey = "test-key")
        )
        return Fixture(server, service)
    }

    private data class Fixture(
        val server: MockRestServiceServer,
        val service: OpenAiRequestDraftService
    )
}
