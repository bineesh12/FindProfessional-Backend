package com.findprofessional.marketplace.user

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class AccountControllerTest {
    @Test
    fun `deletion uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val deletion = mock(AccountDeletionService::class.java)

        AccountController(deletion).delete(
            Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .build()
        )

        verify(deletion).delete(userId)
    }
}
