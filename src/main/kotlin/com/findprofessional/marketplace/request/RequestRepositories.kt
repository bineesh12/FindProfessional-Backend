package com.findprofessional.marketplace.request

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RequestSessionRepository : JpaRepository<RequestSession, UUID> {
    fun findByIdAndCustomerId(id: UUID, customerId: UUID): Optional<RequestSession>
}

interface RequestMessageRepository : JpaRepository<RequestMessage, UUID> {
    fun findAllBySessionIdOrderBySequenceNumberAsc(sessionId: UUID): List<RequestMessage>
    fun findBySessionIdAndQuestionKeyAndSender(
        sessionId: UUID,
        questionKey: String,
        sender: MessageSender
    ): Optional<RequestMessage>
}

interface RequestAnswerRepository : JpaRepository<RequestAnswer, UUID> {
    fun findAllBySessionId(sessionId: UUID): List<RequestAnswer>
    fun findBySessionIdAndQuestionId(sessionId: UUID, questionId: UUID): Optional<RequestAnswer>
}

interface CustomerRequestRepository : JpaRepository<CustomerRequest, UUID> {
    fun findBySessionId(sessionId: UUID): Optional<CustomerRequest>
}

interface RequestLocationRepository : JpaRepository<RequestLocation, UUID>

interface PostcodeCoordinateRepository : JpaRepository<PostcodeCoordinate, UUID> {
    fun findByCountryCodeAndPostalCode(countryCode: String, postalCode: String): Optional<PostcodeCoordinate>
}

interface RequestServiceSuggestionRepository : JpaRepository<RequestServiceSuggestion, UUID> {
    fun findAllBySessionIdOrderByRankAsc(sessionId: UUID): List<RequestServiceSuggestion>
}
