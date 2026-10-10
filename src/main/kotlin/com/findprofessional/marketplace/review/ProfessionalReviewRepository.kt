package com.findprofessional.marketplace.review

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface ProfessionalReviewRepository : JpaRepository<ProfessionalReview, UUID> {
    fun findByRequestId(requestId: UUID): Optional<ProfessionalReview>
    fun findByRequestIdAndCustomerUserId(requestId: UUID, customerUserId: UUID): Optional<ProfessionalReview>
    fun existsByRequestId(requestId: UUID): Boolean
    fun findAllByProfessionalUserIdOrderByCreatedAtDescIdDesc(
        professionalUserId: UUID,
        pageable: Pageable
    ): Page<ProfessionalReview>
    fun countByProfessionalUserId(professionalUserId: UUID): Long

    @Query("SELECT AVG(review.rating) FROM ProfessionalReview review WHERE review.professionalUserId = :professionalId")
    fun averageRating(@Param("professionalId") professionalId: UUID): Double?

    @Query(
        """
        SELECT review.professionalUserId AS professionalId,
               AVG(review.rating) AS averageRating,
               COUNT(review.id) AS reviewCount
        FROM ProfessionalReview review
        WHERE review.professionalUserId IN :professionalIds
        GROUP BY review.professionalUserId
        """
    )
    fun summaries(@Param("professionalIds") professionalIds: Collection<UUID>): List<ProfessionalReviewSummary>

    @Query(
        """
        SELECT review.rating AS rating, COUNT(review.id) AS reviewCount
        FROM ProfessionalReview review
        WHERE review.professionalUserId = :professionalId
        GROUP BY review.rating
        """
    )
    fun ratingBreakdown(@Param("professionalId") professionalId: UUID): List<RatingCount>
}
interface ProfessionalReviewSummary {
    val professionalId: UUID
    val averageRating: Double
    val reviewCount: Long
}
interface RatingCount {
    val rating: Int
    val reviewCount: Long
}
