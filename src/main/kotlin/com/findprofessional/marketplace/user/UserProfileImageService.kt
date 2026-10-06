package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class UserProfileImageService(
    private val users: UserAccountRepository,
    private val storage: UserProfileImageStorage
) {
    @Transactional
    fun upload(userId: UUID, file: MultipartFile): UserResponse {
        val user = requireUser(userId)
        val contentType = file.contentType?.lowercase() ?: invalid("Image type is required")
        val extension = SupportedImageTypes[contentType]
            ?: invalid("Only JPEG, PNG, WebP, HEIC, and HEIF images are supported")
        if (file.isEmpty || file.size > MaxImageBytes) {
            invalid("Profile image must be between 1 byte and 5 MB")
        }
        val bytes = file.bytes
        if (!hasExpectedSignature(contentType, bytes)) invalid("The uploaded file is not a valid image")

        val previousUrl = user.profileImageUrl
        val imageUrl = storage.save(userId, extension, bytes)
        return try {
            user.profileImageUrl = imageUrl
            val response = users.save(user).toResponse()
            previousUrl?.let(storage::delete)
            response
        } catch (error: RuntimeException) {
            storage.delete(imageUrl)
            throw error
        }
    }

    @Transactional
    fun delete(userId: UUID): UserResponse {
        val user = requireUser(userId)
        val previousUrl = user.profileImageUrl
        user.profileImageUrl = null
        val response = users.save(user).toResponse()
        previousUrl?.let(storage::delete)
        return response
    }

    private fun requireUser(userId: UUID): UserAccount = users.findById(userId).orElseThrow {
        AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
    }

    private fun invalid(message: String): Nothing =
        throw AuthException(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST)

    private companion object {
        const val MaxImageBytes = 5L * 1024 * 1024
        val SupportedImageTypes = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/heic" to "heic",
            "image/heif" to "heif"
        )

        fun hasExpectedSignature(contentType: String, bytes: ByteArray): Boolean = when (contentType) {
            "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            "image/png" -> bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
            "image/webp" -> bytes.size >= 12 && bytes.decodeToString(0, 4) == "RIFF" &&
                bytes.decodeToString(8, 12) == "WEBP"
            "image/heic", "image/heif" -> bytes.size >= 12 && bytes.decodeToString(4, 8) == "ftyp"
            else -> false
        }
    }
}
