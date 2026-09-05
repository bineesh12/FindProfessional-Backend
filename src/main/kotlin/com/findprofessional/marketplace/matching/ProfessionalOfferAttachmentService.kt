package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ProfessionalOfferAttachmentService(
    private val authorization: ProfessionalAuthorizationService,
    private val offers: ProfessionalOfferRepository,
    private val attachments: ProfessionalOfferAttachmentRepository,
    private val storage: ProfessionalOfferAttachmentStorage,
    private val storageProperties: PortfolioStorageProperties
) {
    @Transactional
    fun upload(
        userId: UUID,
        requestId: UUID,
        file: MultipartFile
    ): ProfessionalOfferAttachmentResponse {
        authorization.requireProfessional(userId)
        val offer = requireOwnedOffer(userId, requestId)
        if (attachments.countByOfferId(offer.id) >= MaxAttachments) invalid("An offer can contain up to $MaxAttachments attachments")
        val contentType = file.contentType?.lowercase() ?: invalid("Attachment type is required")
        val extension = SupportedTypes[contentType]
            ?: invalid("Only PDF, DOCX, JPEG, and PNG files are supported")
        if (file.isEmpty || file.size > MaxAttachmentBytes) invalid("Attachment must be between 1 byte and 10 MB")
        val bytes = file.bytes
        if (!hasExpectedSignature(contentType, bytes)) invalid("The uploaded attachment has invalid content")
        val filename = file.originalFilename?.takeLast(255)?.ifBlank { "attachment.$extension" }
            ?: "attachment.$extension"
        val stored = storage.save(userId, offer.id, extension, bytes)
        return try {
            attachments.save(
                ProfessionalOfferAttachment(
                    offer = offer,
                    storageKey = stored.storageKey,
                    originalFilename = filename,
                    contentType = contentType,
                    sizeBytes = file.size
                )
            ).toResponse()
        } catch (error: RuntimeException) {
            storage.delete(stored.storageKey)
            throw error
        }
    }

    @Transactional
    fun delete(userId: UUID, requestId: UUID, attachmentId: UUID) {
        authorization.requireProfessional(userId)
        val attachment = attachments.findByIdAndOfferProfessionalUserId(attachmentId, userId)
            .filter { it.offer.request.id == requestId }
            .orElseThrow(::notFound)
        attachments.delete(attachment)
        storage.delete(attachment.storageKey)
    }

    private fun requireOwnedOffer(userId: UUID, requestId: UUID) =
        offers.findByProfessionalUserIdAndRequestId(userId, requestId).orElseThrow {
            ProfessionalOpportunityException(
                "Save the offer before uploading attachments",
                "OFFER_REQUIRED",
                HttpStatus.CONFLICT
            )
        }

    private fun ProfessionalOfferAttachment.toResponse() = ProfessionalOfferAttachmentResponse(
        id = id,
        filename = originalFilename,
        contentType = contentType,
        sizeBytes = sizeBytes,
        url = "${storageProperties.publicPath.trimEnd('/')}/$storageKey"
    )

    private fun invalid(message: String): Nothing = throw ProfessionalOpportunityException(
        message,
        "VALIDATION_ERROR",
        HttpStatus.BAD_REQUEST
    )

    private fun notFound() = ProfessionalOpportunityException(
        "Offer attachment was not found",
        "OFFER_ATTACHMENT_NOT_FOUND",
        HttpStatus.NOT_FOUND
    )

    private companion object {
        const val MaxAttachments = 5L
        const val MaxAttachmentBytes = 10L * 1024 * 1024
        val SupportedTypes = mapOf(
            "application/pdf" to "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
            "image/jpeg" to "jpg",
            "image/png" to "png"
        )

        fun hasExpectedSignature(contentType: String, bytes: ByteArray): Boolean = when (contentType) {
            "application/pdf" -> bytes.size >= 4 && bytes.decodeToString(0, 4) == "%PDF"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
            "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            "image/png" -> bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
            else -> false
        }
    }
}
