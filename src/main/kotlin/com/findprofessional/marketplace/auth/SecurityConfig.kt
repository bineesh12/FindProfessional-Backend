package com.findprofessional.marketplace.auth

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

@Configuration
class SecurityConfig(
    private val authProperties: AuthProperties
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**").permitAll()
                it.requestMatchers("/uploads/**").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtEncoder(): JwtEncoder {
        val key = jwtSecretKey()
        return NimbusJwtEncoder(ImmutableSecret(key))
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey()).macAlgorithm(MacAlgorithm.HS256).build().also {
            it.setJwtValidator(JwtValidators.createDefaultWithIssuer(authProperties.issuer))
        }
    }

    private fun jwtSecretKey(): SecretKeySpec {
        val secret = authProperties.jwtSecret.toByteArray(Charsets.UTF_8)
        require(secret.size >= 32) { "JWT_SECRET must contain at least 32 bytes" }
        return SecretKeySpec(secret, "HmacSHA256")
    }
}
