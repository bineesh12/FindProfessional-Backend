package com.findprofessional.marketplace.subscription

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.auth.oauth2.GoogleCredentials
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.io.ByteArrayInputStream
import java.time.Instant

data class VerifiedStoreSubscription(
    val productId: String,
    val expiresAt: Instant
)

interface GooglePlaySubscriptionVerifier {
    fun verify(purchaseToken: String): VerifiedStoreSubscription
}

@Component
class GooglePlayDeveloperApiSubscriptionVerifier(
    private val properties: SubscriptionProperties,
    private val restClientBuilder: RestClient.Builder
) : GooglePlaySubscriptionVerifier {
    override fun verify(purchaseToken: String): VerifiedStoreSubscription {
        if (properties.googlePlayServiceAccountJson.isBlank()) {
            throw SubscriptionException(
                "Google Play subscription verification is not configured",
                "SUBSCRIPTION_VERIFICATION_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE
            )
        }
        val credentials = GoogleCredentials
            .fromStream(ByteArrayInputStream(properties.googlePlayServiceAccountJson.toByteArray(Charsets.UTF_8)))
            .createScoped(AndroidPublisherScope)
        credentials.refreshIfExpired()
        val accessToken = credentials.accessToken?.tokenValue ?: throw SubscriptionException(
            "Google Play subscription verification is unavailable",
            "SUBSCRIPTION_VERIFICATION_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE
        )

        val response = try {
            restClientBuilder.build().get()
                .uri(
                    "$AndroidPublisherBaseUrl/{packageName}/purchases/subscriptionsv2/tokens/{token}",
                    properties.googlePlayPackageName,
                    purchaseToken
                )
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body(GooglePlaySubscriptionResponse::class.java)
        } catch (_: RestClientResponseException) {
            throw SubscriptionException(
                "Google Play subscription could not be verified",
                "INVALID_STORE_PURCHASE",
                HttpStatus.BAD_REQUEST
            )
        } ?: throw SubscriptionException(
            "Google Play subscription could not be verified",
            "INVALID_STORE_PURCHASE",
            HttpStatus.BAD_REQUEST
        )

        val lineItem = response.lineItems
            .filter { it.productId == properties.googlePlayProductId }
            .maxByOrNull { Instant.parse(it.expiryTime) }
            ?: throw SubscriptionException(
                "The purchase does not contain the Arbio Pro subscription",
                "INVALID_SUBSCRIPTION_PRODUCT",
                HttpStatus.BAD_REQUEST
            )
        val expiresAt = Instant.parse(lineItem.expiryTime)
        val grantsAccess = response.subscriptionState in ActiveStates && expiresAt.isAfter(Instant.now())
        if (!grantsAccess) {
            throw SubscriptionException(
                "The Google Play subscription is not active",
                "SUBSCRIPTION_NOT_ACTIVE",
                HttpStatus.BAD_REQUEST
            )
        }
        return VerifiedStoreSubscription(lineItem.productId, expiresAt)
    }

    private companion object {
        const val AndroidPublisherScope = "https://www.googleapis.com/auth/androidpublisher"
        const val AndroidPublisherBaseUrl =
            "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
        val ActiveStates = setOf(
            "SUBSCRIPTION_STATE_ACTIVE",
            "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
            "SUBSCRIPTION_STATE_CANCELED"
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GooglePlaySubscriptionResponse(
    val subscriptionState: String = "",
    val lineItems: List<GooglePlayLineItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GooglePlayLineItem(
    val productId: String = "",
    val expiryTime: String = Instant.EPOCH.toString()
)
