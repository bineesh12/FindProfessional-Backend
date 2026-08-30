package com.findprofessional.marketplace.question

import com.findprofessional.marketplace.service.marketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class QuestionEngineTest {
    private val repository = mock(ServiceQuestionRepository::class.java)
    private val engine = QuestionEngine(repository)

    @Test
    fun `returns first unanswered question whose condition is satisfied`() {
        val service = marketplaceService()
        val first = question(service, "work_type", 1)
        val conditional = question(service, "repair_details", 2, "work_type", "REPAIR")
        `when`(repository.findAllByServiceIdAndActiveTrueOrderByDisplayOrderAsc(service.id))
            .thenReturn(listOf(first, conditional))

        val next = engine.nextQuestion(service.id, mapOf("work_type" to "REPAIR"))

        assertEquals("repair_details", next?.key)
    }

    @Test
    fun `skips conditional question when controlling answer does not match`() {
        val service = marketplaceService()
        val conditional = question(service, "repair_details", 1, "work_type", "REPAIR")
        `when`(repository.findAllByServiceIdAndActiveTrueOrderByDisplayOrderAsc(service.id))
            .thenReturn(listOf(conditional))

        assertNull(engine.nextQuestion(service.id, mapOf("work_type" to "REPLACE")))
    }

    @Test
    fun `accepts any configured conditional value`() {
        val service = marketplaceService()
        val conditional = question(service, "destination", 1, "packing_scope", "UNPACKING,BOTH")
        `when`(repository.findAllByServiceIdAndActiveTrueOrderByDisplayOrderAsc(service.id))
            .thenReturn(listOf(conditional))

        assertEquals("destination", engine.nextQuestion(service.id, mapOf("packing_scope" to "BOTH"))?.key)
    }
}

private fun question(
    service: com.findprofessional.marketplace.service.MarketplaceService,
    key: String,
    order: Int,
    conditionKey: String? = null,
    conditionValue: String? = null
) = ServiceQuestion(
    service = service,
    key = key,
    prompt = key,
    type = QuestionType.TEXT,
    displayOrder = order,
    conditionQuestionKey = conditionKey,
    conditionValue = conditionValue
)
