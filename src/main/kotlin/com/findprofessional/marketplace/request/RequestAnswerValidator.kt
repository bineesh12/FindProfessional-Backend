package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Currency
import java.util.Locale

internal fun normalizeAnswer(question: ServiceQuestion, rawValue: String): String {
    val value = rawValue.trim()
    return when (question.type) {
        QuestionType.TEXT, QuestionType.LOCATION ->
            value.takeIf { it.length in 2..500 }
        QuestionType.SINGLE_CHOICE ->
            value.takeIf { candidate -> question.options.any { it.value == candidate } }
        QuestionType.NUMBER ->
            value.takeIf { it.toBigDecimalOrNull()?.let { number -> number > BigDecimal.ZERO } == true }
        QuestionType.MONEY ->
            normalizeMoney(value)
        QuestionType.DATE -> parseRequestDate(value)?.toString()
        QuestionType.POSTCODE -> normalizeSwedishPostcode(value)
        QuestionType.BOOLEAN -> value.uppercase().takeIf { it in setOf("TRUE", "FALSE") }
    } ?: throw RequestException("The answer is invalid for this question", "INVALID_ANSWER")
}

private fun normalizeMoney(value: String): String? {
    value.toBigDecimalOrNull()?.let { amount ->
        return amount.takeIf { it >= BigDecimal.ZERO }?.stripTrailingZeros()?.toPlainString()
    }
    val match = MoneyPattern.matchEntire(value) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO }
        ?: return null
    val currency = match.groupValues[2].uppercase()
        .takeIf { runCatching { Currency.getInstance(it) }.isSuccess }
        ?: return null
    return "${amount.stripTrailingZeros().toPlainString()} $currency"
}

private fun normalizeSwedishPostcode(value: String): String? {
    val match = SwedishPostcodePattern.matchEntire(value) ?: return null
    return "${match.groupValues[1]} ${match.groupValues[2]}"
}

private fun parseRequestDate(value: String): LocalDate? {
    fullDateFormats.forEach { format ->
        runCatching { LocalDate.parse(value, format) }.getOrNull()?.let { return it }
    }
    monthFormats.forEach { format ->
        runCatching { YearMonth.parse(value, format).atDay(1) }.getOrNull()?.let { return it }
    }
    return null
}

private fun format(pattern: String): DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern(pattern)
    .toFormatter(Locale.ENGLISH)

private val fullDateFormats = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    format("uuuu/MM/dd"),
    format("uuuu M d")
)

private val monthFormats = listOf(
    format("uuuu-MM"),
    format("uuuu/MM"),
    format("uuuu MMM"),
    format("uuuu MMMM"),
    format("MMM uuuu"),
    format("MMMM uuuu")
)

private val SwedishPostcodePattern = Regex("""^(\d{3})\s?(\d{2})$""")
private val MoneyPattern = Regex("""^(\d+(?:[.,]\d{1,2})?)\s+([A-Za-z]{3})$""")
