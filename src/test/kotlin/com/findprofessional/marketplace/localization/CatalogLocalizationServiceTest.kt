package com.findprofessional.marketplace.localization

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.question.QuestionOption
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class CatalogLocalizationServiceTest {
    private val categories = mock(ServiceCategoryTranslationRepository::class.java)
    private val services = mock(MarketplaceServiceTranslationRepository::class.java)
    private val questions = mock(ServiceQuestionTranslationRepository::class.java)
    private val options = mock(QuestionOptionTranslationRepository::class.java)
    private val localization = CatalogLocalizationService(categories, services, questions, options)

    @Test
    fun `translated service falls back field by field to canonical English`() {
        val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
        val service = MarketplaceService(
            category = category,
            code = "ROOFING",
            name = "Roofing",
            shortDescription = "Roof work",
            iconKey = "roofing"
        )
        `when`(services.findAllByEntityIdInAndLocale(listOf(service.id), "sv")).thenReturn(
            listOf(MarketplaceServiceTranslation(service.id, "sv", "Takläggning", "Takarbete", "tak"))
        )
        `when`(categories.findAllByEntityIdInAndLocale(listOf(category.id), "sv")).thenReturn(emptyList())

        val response = localization.services(listOf(service), "sv").single()

        assertEquals("Takläggning", response.name)
        assertEquals("Takarbete", response.shortDescription)
        assertEquals("Home", response.categoryName)
    }

    @Test
    fun `question option keeps its stable answer value while label is translated`() {
        val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
        val service = MarketplaceService(category = category, code = "ROOFING", name = "Roofing", shortDescription = "Roof work", iconKey = "roofing")
        val question = ServiceQuestion(
            service = service,
            key = "urgency",
            prompt = "How urgent is it?",
            type = QuestionType.SINGLE_CHOICE,
            displayOrder = 1
        )
        val option = QuestionOption(
            question = question,
            value = "WITHIN_WEEK",
            label = "Within a week",
            displayOrder = 1
        )
        question.options += option
        `when`(questions.findAllByEntityIdInAndLocale(listOf(question.id), "sv")).thenReturn(
            listOf(ServiceQuestionTranslation(question.id, "sv", "Hur brådskande är det?", null))
        )
        `when`(options.findAllByEntityIdInAndLocale(listOf(option.id), "sv")).thenReturn(
            listOf(QuestionOptionTranslation(option.id, "sv", "Inom en vecka", null))
        )

        val translated = localization.question(question, "sv")

        assertEquals("Hur brådskande är det?", translated.prompt)
        assertEquals("WITHIN_WEEK", translated.options.single().value)
        assertEquals("Inom en vecka", translated.options.single().label)
    }
}
