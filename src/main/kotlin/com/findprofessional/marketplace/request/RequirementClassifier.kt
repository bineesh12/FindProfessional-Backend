package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.ServiceAlias
import org.springframework.stereotype.Component

data class RankedServiceMatch(
    val service: MarketplaceService,
    val score: Int,
    val confidence: MatchConfidence,
    val matchedPhrase: String?
)

@Component
class RequirementClassifier {
    fun rank(
        description: String,
        services: List<MarketplaceService>,
        aliases: List<ServiceAlias> = emptyList()
    ): List<RankedServiceMatch> {
        val normalized = normalize(description)
        val aliasesByService = aliases.groupBy { it.service.id }
        val scored = services.map { service ->
            val aliasMatches = aliasesByService[service.id].orEmpty()
                .filter { containsPhrase(normalized, normalize(it.phrase)) }
            val bestAlias = aliasMatches.maxByOrNull(ServiceAlias::weight)
            val nameScore = if (containsPhrase(normalized, normalize(service.name))) NameWeight else 0
            val keywordScore = normalize(service.searchKeywords)
                .split(' ')
                .filter { it.length >= MinimumTermLength }
                .distinct()
                .count { containsPhrase(normalized, it) } * KeywordWeight
            Triple(service, maxOf(bestAlias?.weight ?: 0, nameScore) + keywordScore, bestAlias?.phrase)
        }.filter { (_, score, _) -> score > 0 }
            .sortedWith(
                compareByDescending<Triple<MarketplaceService, Int, String?>> { it.second }
                    .thenBy { it.first.name }
            )

        return scored.mapIndexed { index, (service, score, phrase) ->
            val nextScore = scored.getOrNull(index + 1)?.second ?: 0
            RankedServiceMatch(service, score, confidence(score, score - nextScore), phrase)
        }
    }

    fun classify(description: String, services: List<MarketplaceService>): MarketplaceService? =
        rank(description, services).firstOrNull()?.service

    private fun confidence(score: Int, margin: Int): MatchConfidence = when {
        score >= HighScore && margin >= HighMargin -> MatchConfidence.HIGH
        score >= MediumScore -> MatchConfidence.MEDIUM
        else -> MatchConfidence.LOW
    }

    private fun normalize(value: String) = value.lowercase()
        .replace(NonWord, " ")
        .trim()
        .replace(MultipleSpaces, " ")
        .split(' ')
        .joinToString(" ") { token ->
            if (token.length > 4 && token.endsWith('s') && !token.endsWith("ss")) {
                token.dropLast(1)
            } else {
                token
            }
        }

    private fun containsPhrase(description: String, phrase: String): Boolean =
        phrase.isNotBlank() && " $description ".contains(" $phrase ")

    private companion object {
        val NonWord = Regex("[^a-z0-9]+")
        val MultipleSpaces = Regex("\\s+")
        const val MinimumTermLength = 4
        const val NameWeight = 60
        const val KeywordWeight = 10
        const val HighScore = 70
        const val HighMargin = 15
        const val MediumScore = 25
    }
}
