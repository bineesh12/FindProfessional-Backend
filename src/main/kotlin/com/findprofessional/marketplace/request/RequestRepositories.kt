package com.findprofessional.marketplace.request

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable
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

    @Query(
        """
        SELECT request FROM CustomerRequest request
        WHERE request.status = :status
          AND request.service.id IN :serviceIds
          AND request.customerId <> :professionalUserId
          AND NOT EXISTS (
              SELECT decline.id FROM ProfessionalOpportunityDecline decline
              WHERE decline.professionalUserId = :professionalUserId
                AND decline.request = request
          )
        ORDER BY request.createdAt DESC
        """
    )
    fun findProfessionalOpportunities(
        @Param("status") status: CustomerRequestStatus,
        @Param("serviceIds") serviceIds: Collection<UUID>,
        @Param("professionalUserId") professionalUserId: UUID,
        pageable: Pageable
    ): List<CustomerRequest>

    @Query(
        """
        SELECT COUNT(request) FROM CustomerRequest request
        WHERE request.status = :status
          AND request.service.id IN :serviceIds
          AND request.customerId <> :professionalUserId
          AND NOT EXISTS (
              SELECT decline.id FROM ProfessionalOpportunityDecline decline
              WHERE decline.professionalUserId = :professionalUserId
                AND decline.request = request
          )
        """
    )
    fun countProfessionalOpportunities(
        @Param("status") status: CustomerRequestStatus,
        @Param("serviceIds") serviceIds: Collection<UUID>,
        @Param("professionalUserId") professionalUserId: UUID
    ): Long
}

interface RequestLocationRepository : JpaRepository<RequestLocation, UUID> {
    fun findAllByRequestIdIn(requestIds: Collection<UUID>): List<RequestLocation>
}

interface PostcodeCoordinateRepository : JpaRepository<PostcodeCoordinate, UUID> {
    fun findByCountryCodeAndPostalCode(countryCode: String, postalCode: String): Optional<PostcodeCoordinate>
}

interface RequestServiceSuggestionRepository : JpaRepository<RequestServiceSuggestion, UUID> {
    fun findAllBySessionIdOrderByRankAsc(sessionId: UUID): List<RequestServiceSuggestion>
}
