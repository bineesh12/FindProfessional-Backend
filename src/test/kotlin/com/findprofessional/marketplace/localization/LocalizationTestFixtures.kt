package com.findprofessional.marketplace.localization

import com.findprofessional.marketplace.auth.anyValue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.context.support.ResourceBundleMessageSource

fun testCatalogLocalization(): CatalogLocalizationService {
    val categories = mock(ServiceCategoryTranslationRepository::class.java)
    val services = mock(MarketplaceServiceTranslationRepository::class.java)
    val questions = mock(ServiceQuestionTranslationRepository::class.java)
    val options = mock(QuestionOptionTranslationRepository::class.java)
    `when`(categories.findAllByEntityIdInAndLocale(anyValue(), anyValue())).thenReturn(emptyList())
    `when`(services.findAllByEntityIdInAndLocale(anyValue(), anyValue())).thenReturn(emptyList())
    `when`(questions.findAllByEntityIdInAndLocale(anyValue(), anyValue())).thenReturn(emptyList())
    `when`(options.findAllByEntityIdInAndLocale(anyValue(), anyValue())).thenReturn(emptyList())
    return CatalogLocalizationService(categories, services, questions, options)
}

fun testLocaleResolver(locale: String = "en"): RequestLocaleResolver =
    mock(RequestLocaleResolver::class.java).also { `when`(it.current()).thenReturn(locale) }

fun testLocalizedTextService(): LocalizedTextService = LocalizedTextService(
    ResourceBundleMessageSource().apply {
        setBasename("messages")
        setDefaultEncoding("UTF-8")
    }
)
