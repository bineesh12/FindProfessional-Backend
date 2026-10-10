package com.findprofessional.marketplace.localization

import com.findprofessional.marketplace.category.CategoryResponse
import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.question.QuestionOption
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.request.RequestOptionResponse
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.MarketplaceServiceResponse
import org.springframework.stereotype.Service

@Service
class CatalogLocalizationService(
    private val categoryTranslations: ServiceCategoryTranslationRepository,
    private val serviceTranslations: MarketplaceServiceTranslationRepository,
    private val questionTranslations: ServiceQuestionTranslationRepository,
    private val optionTranslations: QuestionOptionTranslationRepository
) {
    fun categories(values: List<ServiceCategory>, locale: String): List<CategoryResponse> {
        if (values.isEmpty()) return emptyList()
        val translated = categoryTranslations.findAllByEntityIdInAndLocale(values.map { it.id }, locale)
            .associateBy { it.entityId }
        return values.map { category ->
            CategoryResponse(category.id, category.code, translated[category.id]?.name ?: category.name, category.iconKey)
        }
    }

    fun services(values: List<MarketplaceService>, locale: String): List<MarketplaceServiceResponse> {
        if (values.isEmpty()) return emptyList()
        val services = serviceTranslations.findAllByEntityIdInAndLocale(values.map { it.id }, locale)
            .associateBy { it.entityId }
        val categories = categoryTranslations.findAllByEntityIdInAndLocale(values.map { it.category.id }.distinct(), locale)
            .associateBy { it.entityId }
        return values.map { service ->
            val translated = services[service.id]
            MarketplaceServiceResponse(
                service.id,
                service.code,
                translated?.name ?: service.name,
                translated?.shortDescription ?: service.shortDescription,
                service.iconKey,
                service.imageUrl,
                service.category.id,
                categories[service.category.id]?.name ?: service.category.name
            )
        }
    }

    fun question(value: ServiceQuestion, locale: String): LocalizedQuestion {
        val question = questionTranslations.findAllByEntityIdInAndLocale(listOf(value.id), locale).firstOrNull()
        val options = if (value.options.isEmpty()) emptyMap() else {
            optionTranslations.findAllByEntityIdInAndLocale(value.options.map { it.id }, locale)
                .associateBy { it.entityId }
        }
        return LocalizedQuestion(
            prompt = question?.prompt ?: value.prompt,
            helperText = question?.helperText ?: value.helperText,
            options = value.options.map { option -> option.toResponse(options[option.id]) }
        )
    }

    private fun QuestionOption.toResponse(translation: QuestionOptionTranslation?) = RequestOptionResponse(
        value = value,
        label = translation?.label ?: label,
        description = translation?.description ?: description
    )
}

data class LocalizedQuestion(
    val prompt: String,
    val helperText: String?,
    val options: List<RequestOptionResponse>
)
