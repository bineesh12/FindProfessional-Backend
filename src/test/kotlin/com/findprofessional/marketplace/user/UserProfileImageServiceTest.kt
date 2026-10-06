package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

class UserProfileImageServiceTest {
    private val users = mock(UserAccountRepository::class.java)
    private val storage = mock(UserProfileImageStorage::class.java)
    private val service = UserProfileImageService(users, storage)

    @Test
    fun `upload validates and replaces the owned profile image`() {
        val user = UserAccount(
            displayName = "Bineesh",
            profileImageUrl = "/uploads/profiles/old.png"
        )
        val bytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        val file = MockMultipartFile("file", "profile.png", "image/png", bytes)
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(storage.save(user.id, "png", bytes)).thenReturn("/uploads/profiles/new.png")
        `when`(users.save(user)).thenReturn(user)

        val response = service.upload(user.id, file)

        assertEquals("/uploads/profiles/new.png", response.profileImageUrl)
        verify(storage).delete("/uploads/profiles/old.png")
    }

    @Test
    fun `upload rejects content that is not a real image`() {
        val user = UserAccount(displayName = "Bineesh")
        val file = MockMultipartFile("file", "profile.png", "image/png", "not-image".encodeToByteArray())
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        val error = assertThrows(AuthException::class.java) { service.upload(user.id, file) }

        assertEquals("VALIDATION_ERROR", error.code)
    }

    @Test
    fun `delete clears the owned profile image`() {
        val user = UserAccount(
            displayName = "Bineesh",
            profileImageUrl = "/uploads/profiles/profile.jpg"
        )
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        val response = service.delete(user.id)

        assertNull(response.profileImageUrl)
        verify(storage).delete("/uploads/profiles/profile.jpg")
    }
}
