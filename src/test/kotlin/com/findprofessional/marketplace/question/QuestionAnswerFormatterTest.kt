package com.findprofessional.marketplace.question

import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.service.MarketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestionAnswerFormatterTest {
    @Test
    fun `numeric answer includes configured unit`() {
        val service = MarketplaceService(
            category = category(),
            code = "FLOORING",
            name = "Flooring",
            shortDescription = "Install flooring",
            iconKey = "flooring"
        )
        val question = ServiceQuestion(
            service = service,
            key = "floor_area",
            prompt = "Approximately how large is the floor area?",
            type = QuestionType.NUMBER,
            unit = "m²",
            displayOrder = 1
        )

        assertEquals("120 m²", question.displayAnswer("120"))
    }
}
