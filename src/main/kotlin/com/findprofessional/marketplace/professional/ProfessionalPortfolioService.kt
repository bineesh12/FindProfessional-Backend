package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.service.MarketplaceServiceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ProfessionalPortfolioService(
    private val projects: PortfolioProjectRepository,
    private val images: PortfolioImageRepository,
    private val services: MarketplaceServiceRepository,
    private val offerings: ProfessionalServiceOfferingRepository,
    private val profiles: ProfessionalProfileRepository,
    private val authorization: ProfessionalAuthorizationService,
    private val storage: PortfolioImageStorage,
    private val storageProperties: PortfolioStorageProperties
) {
    @Transactional(readOnly = true)
    fun list(userId: UUID): List<PortfolioProjectResponse> {
        authorization.requireProfessional(userId)
        return loadProjects(userId)
    }

    @Transactional
    fun create(userId: UUID, request: SavePortfolioProjectRequest): PortfolioProjectResponse {
        authorization.requireProfessional(userId)
        if (!profiles.existsById(userId)) {
            throw ProfessionalProfileException(
                "Save the professional profile before adding portfolio projects",
                "PROFILE_REQUIRED",
                HttpStatus.CONFLICT
            )
        }
        val profileProjects = projects.findAllByProfessionalUserIdOrderByDisplayOrderAscCreatedAtAsc(userId)
        if (profileProjects.size >= MaxProjects) invalid("A portfolio can contain up to $MaxProjects projects")
        val normalized = request.normalized()
        val service = requireOfferedService(userId, normalized.serviceId)
        val project = projects.save(
            PortfolioProject(
                professionalUserId = userId,
                service = service,
                title = normalized.title,
                description = normalized.description,
                displayOrder = profileProjects.size
            )
        )
        return project.toResponse(emptyList())
    }

    @Transactional
    fun update(userId: UUID, projectId: UUID, request: SavePortfolioProjectRequest): PortfolioProjectResponse {
        authorization.requireProfessional(userId)
        val project = requireOwnedProject(userId, projectId)
        val normalized = request.normalized()
        project.service = requireOfferedService(userId, normalized.serviceId)
        project.title = normalized.title
        project.description = normalized.description
        val saved = projects.save(project)
        val projectImages = images.findAllByProjectIdInOrderByDisplayOrderAscCreatedAtAsc(listOf(projectId))
        return saved.toResponse(projectImages)
    }

    @Transactional
    fun delete(userId: UUID, projectId: UUID) {
        authorization.requireProfessional(userId)
        val project = requireOwnedProject(userId, projectId)
        images.findAllByProjectIdInOrderByDisplayOrderAscCreatedAtAsc(listOf(projectId))
            .forEach { storage.delete(it.storageKey) }
        projects.delete(project)
    }

    @Transactional
    fun reorder(userId: UUID, request: ReorderPortfolioProjectsRequest): List<PortfolioProjectResponse> {
        authorization.requireProfessional(userId)
        val current = projects.findAllByProfessionalUserIdOrderByDisplayOrderAscCreatedAtAsc(userId)
        if (request.projectIds.size != request.projectIds.distinct().size ||
            request.projectIds.toSet() != current.map { it.id }.toSet()
        ) {
            invalid("Portfolio order must contain every owned project exactly once")
        }
        val byId = current.associateBy { it.id }
        request.projectIds.forEachIndexed { index, id -> checkNotNull(byId[id]).displayOrder = index }
        projects.saveAll(current)
        return loadProjects(userId)
    }

    @Transactional
    fun uploadImage(userId: UUID, projectId: UUID, file: MultipartFile): PortfolioImageResponse {
        authorization.requireProfessional(userId)
        val project = requireOwnedProject(userId, projectId)
        val imageCount = images.countByProjectId(projectId)
        if (imageCount >= MaxImagesPerProject) invalid("A project can contain up to $MaxImagesPerProject images")
        val contentType = file.contentType?.lowercase() ?: invalid("Image type is required")
        val extension = SupportedImageTypes[contentType]
            ?: invalid("Only JPEG, PNG, WebP, HEIC, and HEIF images are supported")
        if (file.isEmpty || file.size > MaxImageBytes) invalid("Image must be between 1 byte and 5 MB")
        val bytes = file.bytes
        if (!hasExpectedSignature(contentType, bytes)) invalid("The uploaded file is not a valid image")
        val originalFilename = file.originalFilename?.takeLast(255)?.ifBlank { "portfolio.$extension" }
            ?: "portfolio.$extension"
        val stored = storage.save(userId, projectId, extension, bytes)
        return try {
            images.save(
                PortfolioImage(
                    project = project,
                    storageKey = stored.storageKey,
                    originalFilename = originalFilename,
                    contentType = contentType,
                    sizeBytes = file.size,
                    displayOrder = imageCount.toInt()
                )
            ).toResponse()
        } catch (error: RuntimeException) {
            storage.delete(stored.storageKey)
            throw error
        }
    }

    @Transactional
    fun deleteImage(userId: UUID, imageId: UUID) {
        authorization.requireProfessional(userId)
        val image = images.findByIdAndProjectProfessionalUserId(imageId, userId).orElseThrow(::notFound)
        images.delete(image)
        storage.delete(image.storageKey)
    }

    internal fun loadProjects(userId: UUID): List<PortfolioProjectResponse> {
        val profileProjects = projects.findAllByProfessionalUserIdOrderByDisplayOrderAscCreatedAtAsc(userId)
        val projectImages = if (profileProjects.isEmpty()) {
            emptyList()
        } else {
            images.findAllByProjectIdInOrderByDisplayOrderAscCreatedAtAsc(profileProjects.map { it.id })
        }
        val imagesByProject = projectImages
            .groupBy { it.project.id }
        return profileProjects.map { it.toResponse(imagesByProject[it.id].orEmpty()) }
    }

    private fun requireOwnedProject(userId: UUID, projectId: UUID) =
        projects.findByIdAndProfessionalUserId(projectId, userId).orElseThrow(::notFound)

    private fun requireOfferedService(userId: UUID, serviceId: UUID): com.findprofessional.marketplace.service.MarketplaceService {
        val isOffered = offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(userId)
            .any { it.service.id == serviceId }
        if (!isOffered) {
            throw ProfessionalProfileException(
                "Portfolio projects must use one of the professional's offered services",
                "SERVICE_NOT_OFFERED",
                HttpStatus.BAD_REQUEST
            )
        }
        return services.findById(serviceId)
            .filter { it.active }
            .orElseThrow {
                ProfessionalProfileException("Service was not found", "SERVICE_NOT_FOUND", HttpStatus.NOT_FOUND)
            }
    }

    private fun SavePortfolioProjectRequest.normalized(): SavePortfolioProjectRequest {
        val normalized = copy(title = title.trim(), description = description.trim())
        if (normalized.title.length !in 2..120 || normalized.description.length !in 10..500) {
            invalid("Portfolio project details are invalid")
        }
        return normalized
    }

    private fun invalid(message: String): Nothing =
        throw ProfessionalProfileException(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST)

    private fun notFound() =
        ProfessionalProfileException("Portfolio item was not found", "PORTFOLIO_NOT_FOUND", HttpStatus.NOT_FOUND)

    private fun PortfolioProject.toResponse(projectImages: List<PortfolioImage>) = PortfolioProjectResponse(
        id = id,
        title = title,
        description = description,
        service = ProfessionalServiceOptionResponse(service.id, service.name, service.category.name),
        images = projectImages.map { it.toResponse() }
    )

    private fun PortfolioImage.toResponse() = PortfolioImageResponse(
        id = id,
        url = "${storageProperties.publicPath.trimEnd('/')}/$storageKey",
        contentType = contentType,
        displayOrder = displayOrder
    )

    private companion object {
        const val MaxProjects = 12
        const val MaxImagesPerProject = 6L
        const val MaxImageBytes = 5L * 1024 * 1024
        val SupportedImageTypes = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/heic" to "heic",
            "image/heif" to "heif"
        )

        fun hasExpectedSignature(contentType: String, bytes: ByteArray): Boolean = when (contentType) {
            "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            "image/png" -> bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
            "image/webp" -> bytes.size >= 12 && bytes.decodeToString(0, 4) == "RIFF" && bytes.decodeToString(8, 12) == "WEBP"
            "image/heic", "image/heif" -> bytes.size >= 12 && bytes.decodeToString(4, 8) == "ftyp"
            else -> false
        }
    }
}
