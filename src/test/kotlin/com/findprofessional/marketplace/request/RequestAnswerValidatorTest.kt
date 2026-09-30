package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.service.MarketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test

class RequestAnswerValidatorTest {
    private val service = MarketplaceService(
        category = category(),
        code = "HOUSE_CONSTRUCTION",
        name = "House construction",
        shortDescription = "Build a home",
        iconKey = "construction"
    )
    private val question = ServiceQuestion(
        service = service,
        key = "preferred_start_date",
        prompt = "When should construction begin?",
        type = QuestionType.DATE,
        displayOrder = 1
    )

    @ParameterizedTest
    @CsvSource(
        "2027-01-01, 2027-01-01",
        "2027/01/01, 2027-01-01",
        "'2027 jan', 2027-01-01",
        "'January 2027', 2027-01-01"
    )
    fun `accepted dates are stored in ISO format`(input: String, expected: String) {
        assertEquals(expected, normalizeAnswer(question, input))
    }

    @Test
    fun `invalid date is rejected`() {
        assertThrows(RequestException::class.java) {
            normalizeAnswer(question, "sometime next year")
        }
    }

    @ParameterizedTest
    @CsvSource(
        "41833, '418 33'",
        "'418 33', '418 33'"
    )
    fun `swedish postcodes are normalized`(input: String, expected: String) {
        assertEquals(expected, normalizeAnswer(postcodeQuestion(), input))
    }

    @ParameterizedTest
    @CsvSource("4183", "418333", "'418 AB'", "'SE-418 33'")
    fun `invalid swedish postcodes are rejected`(input: String) {
        assertThrows(RequestException::class.java) {
            normalizeAnswer(postcodeQuestion(), input)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "'15000 SEK', '15000 SEK'",
        "'15000,50 eur', '15000.5 EUR'",
        "'1250.00 nok', '1250 NOK'"
    )
    fun `money answers are normalized with ISO currency`(input: String, expected: String) {
        assertEquals(expected, normalizeAnswer(moneyQuestion(), input))
    }

    @ParameterizedTest
    @CsvSource("'15000 INVALID'", "'amount SEK'", "'-10 SEK'", "'100 XYZ'")
    fun `invalid money answers are rejected`(input: String) {
        assertThrows(RequestException::class.java) {
            normalizeAnswer(moneyQuestion(), input)
        }
    }

    private fun postcodeQuestion() = ServiceQuestion(
        service = service,
        key = "service_postcode",
        prompt = "What is the postcode?",
        type = QuestionType.POSTCODE,
        displayOrder = 1
    )

    private fun moneyQuestion() = ServiceQuestion(
        service = service,
        key = "budget",
        prompt = "What approximate budget are you considering?",
        type = QuestionType.MONEY,
        displayOrder = 1
    )
}
