package com.findprofessional.marketplace.question

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class QuestionEngine(
    private val questions: ServiceQuestionRepository
) {
    fun nextQuestion(serviceId: UUID, answers: Map<String, String>): ServiceQuestion? =
        questions.findAllByServiceIdAndActiveTrueOrderByDisplayOrderAsc(serviceId)
            .firstOrNull { question ->
                question.key !in answers && conditionSatisfied(question, answers)
            }

    fun allQuestions(serviceId: UUID): List<ServiceQuestion> =
        questions.findAllByServiceIdAndActiveTrueOrderByDisplayOrderAsc(serviceId)

    private fun conditionSatisfied(question: ServiceQuestion, answers: Map<String, String>): Boolean {
        val sourceKey = question.conditionQuestionKey ?: return true
        val allowedValues = question.conditionValue?.split(',')?.map(String::trim).orEmpty()
        return answers[sourceKey] in allowedValues
    }
}
