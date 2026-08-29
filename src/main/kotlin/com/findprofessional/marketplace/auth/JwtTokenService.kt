package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccount
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
open class JwtTokenService(
    private val jwtEncoder: JwtEncoder,
    private val authProperties: AuthProperties,
    private val clock: Clock = Clock.systemUTC()
) {
    open fun createAccessToken(user: UserAccount): String {
        val now = Instant.now(clock)
        val claims = JwtClaimsSet.builder()
            .issuer(authProperties.issuer)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(authProperties.accessTokenMinutes * 60))
            .subject(user.id.toString())
            .claim("token_type", "access")
            .build()

        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    fun accessTokenExpiresInSeconds(): Long = authProperties.accessTokenMinutes * 60
}
