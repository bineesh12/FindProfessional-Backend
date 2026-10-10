package com.findprofessional.marketplace.subscription

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.UUID

@ConfigurationProperties(prefix = "app.subscription")
data class SubscriptionProperties(
    val enabled: Boolean = false,
    val freeOpportunityLimit: Int = 5,
    val googlePlayPackageName: String = "com.getarbio.app",
    val googlePlayProductId: String = "arbio_pro_monthly",
    val googlePlayServiceAccountJson: String = "",
    val appStoreBundleId: String = "com.getarbio.app",
    val appStoreProductId: String = "arbio_pro_monthly",
    val appStoreSharedSecret: String = "",
    val adminUserIds: Set<UUID> = emptySet()
) {
    init {
        require(freeOpportunityLimit >= 0) { "Free opportunity limit must not be negative" }
    }
}
