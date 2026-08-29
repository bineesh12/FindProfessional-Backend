package com.findprofessional.marketplace.user

import jakarta.validation.constraints.NotNull
import java.util.UUID

enum class RoleSelection {
    CUSTOMER,
    PROFESSIONAL,
    BOTH;

    fun toRoles(): Set<UserRole> = when (this) {
        CUSTOMER -> setOf(UserRole.CUSTOMER)
        PROFESSIONAL -> setOf(UserRole.PROFESSIONAL)
        BOTH -> setOf(UserRole.CUSTOMER, UserRole.PROFESSIONAL)
    }
}

data class SelectRoleRequest(
    @field:NotNull
    val role: RoleSelection
)

data class UserResponse(
    val id: UUID,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String,
    val roles: Set<UserRole>,
    val phoneVerified: Boolean,
    val roleSelectionRequired: Boolean
)

fun UserAccount.toResponse() = UserResponse(
    id = id,
    email = email,
    phoneNumber = phoneNumber,
    displayName = displayName,
    roles = roles,
    phoneVerified = phoneVerified,
    roleSelectionRequired = roleSelectedAt == null
)
