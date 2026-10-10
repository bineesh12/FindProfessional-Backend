package com.findprofessional.marketplace.professional

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class ProfessionalVerificationControllerTest {
    private val service = mock(ProfessionalVerificationService::class.java)
    private val controller = ProfessionalVerificationController(service)

    @Test
    fun `submission uses authenticated professional`() {
        val userId = UUID.randomUUID()
        val request = SubmitProfessionalVerificationRequest(
            ProfessionalBusinessType.COMPANY,
            "SE",
            "556123-4567",
            true
        )
        val expected = ProfessionalVerificationResponse(
            ProfessionalVerificationStatus.PENDING,
            ProfessionalBusinessType.COMPANY,
            "SE",
            "556123-4567",
            true
        )
        `when`(service.submit(userId, request)).thenReturn(expected)

        assertEquals(expected, controller.submit(jwt(userId), request))
    }
}

private fun jwt(userId: UUID) = Jwt.withTokenValue("access-token")
    .header("alg", "HS256")
    .subject(userId.toString())
    .build()
