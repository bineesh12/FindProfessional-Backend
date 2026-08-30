package com.findprofessional.marketplace.question

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "question_options")
class QuestionOption(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    val question: ServiceQuestion,

    @Column(name = "option_value", nullable = false)
    val value: String,

    @Column(nullable = false)
    val label: String,

    val description: String? = null,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int
)
