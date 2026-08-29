package com.findprofessional.marketplace.user

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserAccount(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", unique = true)
    var email: String? = null,

    @Column(name = "phone_number", unique = true)
    var phoneNumber: String? = null,

    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "google_subject", unique = true)
    var googleSubject: String? = null,

    @Column(name = "firebase_uid", unique = true)
    var firebaseUid: String? = null,

    @Column(name = "phone_verified", nullable = false)
    var phoneVerified: Boolean = false,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    var roles: MutableSet<UserRole> = mutableSetOf(UserRole.CUSTOMER),

    @Column(name = "role_selected_at")
    var roleSelectedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PrePersist
    fun beforeInsert() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun beforeUpdate() {
        updatedAt = Instant.now()
    }
}
