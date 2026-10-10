package com.findprofessional.marketplace.subscription

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.io.IOException
import java.time.Instant

class AppStoreSubscriptionVerifierTest {
    @Test
    fun `active production subscription is verified`() {
        val fixture = fixture()
        fixture.server.expect(requestTo("https://buy.itunes.apple.com/verifyReceipt"))
            .andRespond(withSuccess(validReceipt(), MediaType.APPLICATION_JSON))

        val verified = fixture.verifier.verify("receipt-data")

        assertEquals("arbio_pro_monthly", verified.productId)
        assertEquals("original-transaction", verified.originalTransactionId)
        assertEquals(Instant.parse("2100-01-01T00:00:00Z"), verified.expiresAt)
        fixture.server.verify()
    }

    @Test
    fun `sandbox receipt retries against sandbox endpoint`() {
        val fixture = fixture()
        fixture.server.expect(requestTo("https://buy.itunes.apple.com/verifyReceipt"))
            .andRespond(withSuccess("""{"status":21007}""", MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo("https://sandbox.itunes.apple.com/verifyReceipt"))
            .andRespond(withSuccess(validReceipt(), MediaType.APPLICATION_JSON))

        val verified = fixture.verifier.verify("sandbox-receipt")

        assertEquals("original-transaction", verified.originalTransactionId)
        fixture.server.verify()
    }

    @Test
    fun `apple transport failure is reported as temporarily unavailable`() {
        val fixture = fixture()
        fixture.server.expect(requestTo("https://buy.itunes.apple.com/verifyReceipt"))
            .andRespond(withException(IOException("Apple unavailable")))

        val exception = assertThrows(SubscriptionException::class.java) {
            fixture.verifier.verify("receipt-data")
        }

        assertEquals("SUBSCRIPTION_VERIFICATION_UNAVAILABLE", exception.code)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status)
        fixture.server.verify()
    }

    private fun fixture(): Fixture {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val properties = SubscriptionProperties(
            enabled = true,
            appStoreBundleId = "com.getarbio.app",
            appStoreProductId = "arbio_pro_monthly",
            appStoreSharedSecret = "shared-secret"
        )
        return Fixture(AppleReceiptSubscriptionVerifier(properties, builder), server)
    }

    private fun validReceipt() = """{
      "status": 0,
      "receipt": {"bundle_id": "com.getarbio.app"},
      "latest_receipt_info": [{
        "product_id": "arbio_pro_monthly",
        "original_transaction_id": "original-transaction",
        "expires_date_ms": "4102444800000"
      }]
    }""".trimIndent()

    private data class Fixture(
        val verifier: AppleReceiptSubscriptionVerifier,
        val server: MockRestServiceServer
    )
}
