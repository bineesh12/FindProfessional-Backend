package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccount
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

data class IssuedRefreshToken(
    val value: String,
    val entity: RefreshToken
)

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authProperties: AuthProperties,
    private val clock: Clock = Clock.systemUTC()
) {
    private val secureRandom = SecureRandom()

    fun issue(user: UserAccount, familyId: UUID = UUID.randomUUID()): IssuedRefreshToken {
        val value = ByteArray(64).also(secureRandom::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val now = Instant.now(clock)
        val entity = refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = hash(value),
                familyId = familyId,
                expiresAt = now.plusSeconds(authProperties.refreshTokenDays * 86_400),
                createdAt = now
            )
        )
        return IssuedRefreshToken(value, entity)
    }

    fun rotate(value: String): IssuedRefreshToken {
        val now = Instant.now(clock)
        val current = refreshTokenRepository.findByTokenHash(hash(value)).orElseThrow(::invalidRefreshToken)
        if (!current.isActive(now)) {
            revokeFamily(current.familyId, now)
            throw invalidRefreshToken()
        }

        val replacement = issue(current.user, current.familyId)
        current.usedAt = now
        current.replacedByTokenId = replacement.entity.id
        refreshTokenRepository.save(current)
        return replacement
    }

    fun revoke(value: String) {
        val now = Instant.now(clock)
        refreshTokenRepository.findByTokenHash(hash(value)).ifPresent { token ->
            revokeFamily(token.familyId, now)
        }
    }

    private fun revokeFamily(familyId: UUID, now: Instant) {
        val activeFamily = refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId)
        activeFamily.forEach { it.revokedAt = now }
        refreshTokenRepository.saveAll(activeFamily)
    }

    private fun hash(value: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    )

    private fun invalidRefreshToken() = AuthException(
        message = "Refresh token is invalid or expired",
        code = "INVALID_REFRESH_TOKEN",
        status = HttpStatus.UNAUTHORIZED
    )
}
