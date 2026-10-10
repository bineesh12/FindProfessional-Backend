package com.findprofessional.marketplace.subscription

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Instant

data class VerifiedAppStoreSubscription(
    val productId: String,
    val originalTransactionId: String,
    val expiresAt: Instant
)

interface AppStoreSubscriptionVerifier {
    fun verify(receiptData: String): VerifiedAppStoreSubscription
}

@Component
class AppleReceiptSubscriptionVerifier(
    private val properties: SubscriptionProperties,
    private val restClientBuilder: RestClient.Builder
) : AppStoreSubscriptionVerifier {
    override fun verify(receiptData: String): VerifiedAppStoreSubscription {
        if (properties.appStoreSharedSecret.isBlank()) {
            throw SubscriptionException(
                "App Store subscription verification is not configured",
                "SUBSCRIPTION_VERIFICATION_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE
            )
        }

        val production = request(ProductionUrl, receiptData)
        val response = if (production.status == SandboxReceiptSentToProduction) {
            request(SandboxUrl, receiptData)
        } else {
            production
        }
        if (response.status != ValidReceiptStatus || response.receipt.bundleId != properties.appStoreBundleId) {
            throw invalidPurchase()
        }

        val purchase = response.latestReceiptInfo
            .asSequence()
            .filter { it.productId == properties.appStoreProductId && it.cancellationDateMs == null }
            .mapNotNull { item ->
                item.expiresDateMs.toLongOrNull()?.let { item to Instant.ofEpochMilli(it) }
            }
            .maxByOrNull { it.second }
            ?: throw SubscriptionException(
                "The receipt does not contain the Arbio Pro subscription",
                "INVALID_SUBSCRIPTION_PRODUCT",
                HttpStatus.BAD_REQUEST
            )
        if (!purchase.second.isAfter(Instant.now())) {
            throw SubscriptionException(
                "The App Store subscription is not active",
                "SUBSCRIPTION_NOT_ACTIVE",
                HttpStatus.BAD_REQUEST
            )
        }
        return VerifiedAppStoreSubscription(
            productId = purchase.first.productId,
            originalTransactionId = purchase.first.originalTransactionId,
            expiresAt = purchase.second
        )
    }

    private fun request(url: String, receiptData: String): AppStoreReceiptResponse = try {
        restClientBuilder.build().post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                AppStoreReceiptRequest(
                    receiptData = receiptData,
                    password = properties.appStoreSharedSecret
                )
            )
            .retrieve()
            .body(AppStoreReceiptResponse::class.java)
            ?: throw invalidPurchase()
    } catch (_: RestClientResponseException) {
        throw invalidPurchase()
    } catch (_: RestClientException) {
        throw SubscriptionException(
            "App Store subscription verification is temporarily unavailable",
            "SUBSCRIPTION_VERIFICATION_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE
        )
    }

    private fun invalidPurchase() = SubscriptionException(
        "App Store subscription could not be verified",
        "INVALID_STORE_PURCHASE",
        HttpStatus.BAD_REQUEST
    )

    private companion object {
        const val ProductionUrl = "https://buy.itunes.apple.com/verifyReceipt"
        const val SandboxUrl = "https://sandbox.itunes.apple.com/verifyReceipt"
        const val ValidReceiptStatus = 0
        const val SandboxReceiptSentToProduction = 21007
    }
}

private data class AppStoreReceiptRequest(
    @JsonProperty("receipt-data")
    val receiptData: String,
    val password: String,
    @JsonProperty("exclude-old-transactions")
    val excludeOldTransactions: Boolean = true
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AppStoreReceiptResponse(
    val status: Int = -1,
    val receipt: AppStoreReceipt = AppStoreReceipt(),
    @JsonProperty("latest_receipt_info")
    val latestReceiptInfo: List<AppStoreReceiptItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AppStoreReceipt(
    @JsonProperty("bundle_id")
    val bundleId: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AppStoreReceiptItem(
    @JsonProperty("product_id")
    val productId: String = "",
    @JsonProperty("original_transaction_id")
    val originalTransactionId: String = "",
    @JsonProperty("expires_date_ms")
    val expiresDateMs: String = "",
    @JsonProperty("cancellation_date_ms")
    val cancellationDateMs: String? = null
)
