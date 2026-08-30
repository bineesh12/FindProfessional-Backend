package com.findprofessional.marketplace.question

import com.findprofessional.marketplace.service.MarketplaceService
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "service_questions")
class ServiceQuestion(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    val service: MarketplaceService,

    @Column(name = "question_key", nullable = false)
    val key: String,

    @Column(nullable = false)
    val prompt: String,

    @Column(name = "helper_text")
    val helperText: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    val type: QuestionType,

    @Column(nullable = false)
    val required: Boolean = true,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "condition_question_key")
    val conditionQuestionKey: String? = null,

    @Column(name = "condition_value")
    val conditionValue: String? = null,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    val options: MutableList<QuestionOption> = mutableListOf()
)
