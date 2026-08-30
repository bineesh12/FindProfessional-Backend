package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.service.MarketplaceService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

enum class MatchConfidence { HIGH, MEDIUM, LOW }
enum class MatchSource { DETERMINISTIC, AI }

@Entity
@Table(name = "request_service_suggestions")
class RequestServiceSuggestion(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    val session: RequestSession,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    val service: MarketplaceService,
    @Column(nullable = false)
    val score: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val confidence: MatchConfidence,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: MatchSource,
    @Column(name = "matched_phrase")
    val matchedPhrase: String? = null,
    @Column(name = "suggestion_rank", nullable = false)
    val rank: Int,
    @Column(nullable = false)
    var selected: Boolean = false
)
