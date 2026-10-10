package com.findprofessional.marketplace.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

fun interface IdentityDeletionGateway {
    fun delete(firebaseUid: String?)
}

@Configuration
class FirebaseIdentityDeletionConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "app.notifications.firebase", name = ["enabled"], havingValue = "true")
    fun firebaseIdentityDeletionGateway(app: FirebaseApp): IdentityDeletionGateway = IdentityDeletionGateway { uid ->
        if (uid.isNullOrBlank()) return@IdentityDeletionGateway
        try {
            FirebaseAuth.getInstance(app).deleteUser(uid)
        } catch (error: FirebaseAuthException) {
            if (error.authErrorCode != AuthErrorCode.USER_NOT_FOUND) throw error
        }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app.notifications.firebase",
        name = ["enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun noOpIdentityDeletionGateway(): IdentityDeletionGateway = IdentityDeletionGateway { }
}
