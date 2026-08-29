package com.findprofessional.marketplace.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "phone_verifications")
class PhoneVerification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "phone_number", nullable = false)
    val phoneNumber: String,

    @Column(name = "code_hash", nullable = false)
    val codeHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
