package com.findprofessional.marketplace.localization

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.io.Serializable
import java.util.UUID

data class TranslationId(var entityId: UUID = UUID(0, 0), var locale: String = "") : Serializable

@Entity
@Table(name = "service_category_translations")
@IdClass(TranslationId::class)
class ServiceCategoryTranslation(
    @Id @Column(name = "category_id") val entityId: UUID,
    @Id @Column(nullable = false, length = 10) val locale: String,
    @Column(nullable = false) val name: String
)

@Entity
@Table(name = "marketplace_service_translations")
@IdClass(TranslationId::class)
class MarketplaceServiceTranslation(
    @Id @Column(name = "service_id") val entityId: UUID,
    @Id @Column(nullable = false, length = 10) val locale: String,
    @Column(nullable = false) val name: String,
    @Column(name = "short_description", nullable = false) val shortDescription: String,
    @Column(name = "search_keywords", nullable = false) val searchKeywords: String
)

@Entity
@Table(name = "service_question_translations")
@IdClass(TranslationId::class)
class ServiceQuestionTranslation(
    @Id @Column(name = "question_id") val entityId: UUID,
    @Id @Column(nullable = false, length = 10) val locale: String,
    @Column(nullable = false) val prompt: String,
    @Column(name = "helper_text") val helperText: String?
)

@Entity
@Table(name = "question_option_translations")
@IdClass(TranslationId::class)
class QuestionOptionTranslation(
    @Id @Column(name = "option_id") val entityId: UUID,
    @Id @Column(nullable = false, length = 10) val locale: String,
    @Column(nullable = false) val label: String,
    val description: String?
)

interface ServiceCategoryTranslationRepository : JpaRepository<ServiceCategoryTranslation, TranslationId> {
    fun findAllByEntityIdInAndLocale(ids: Collection<UUID>, locale: String): List<ServiceCategoryTranslation>
}

interface MarketplaceServiceTranslationRepository : JpaRepository<MarketplaceServiceTranslation, TranslationId> {
    fun findAllByEntityIdInAndLocale(ids: Collection<UUID>, locale: String): List<MarketplaceServiceTranslation>
}

interface ServiceQuestionTranslationRepository : JpaRepository<ServiceQuestionTranslation, TranslationId> {
    fun findAllByEntityIdInAndLocale(ids: Collection<UUID>, locale: String): List<ServiceQuestionTranslation>
}

interface QuestionOptionTranslationRepository : JpaRepository<QuestionOptionTranslation, TranslationId> {
    fun findAllByEntityIdInAndLocale(ids: Collection<UUID>, locale: String): List<QuestionOptionTranslation>
}
