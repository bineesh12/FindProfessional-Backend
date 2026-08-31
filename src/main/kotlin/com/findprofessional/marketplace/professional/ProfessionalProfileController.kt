package com.findprofessional.marketplace.professional

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/professional-profile")
class ProfessionalProfileController(
    private val profileService: ProfessionalProfileService,
    private val portfolioService: ProfessionalPortfolioService
) {
    @GetMapping
    fun setup(@AuthenticationPrincipal jwt: Jwt): ProfessionalProfileSetupResponse =
        profileService.setup(UUID.fromString(jwt.subject))

    @PutMapping
    fun save(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SaveProfessionalProfileRequest
    ): ProfessionalProfileResponse = profileService.save(UUID.fromString(jwt.subject), request)

    @PostMapping("/portfolio")
    fun createPortfolioProject(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SavePortfolioProjectRequest
    ): PortfolioProjectResponse = portfolioService.create(UUID.fromString(jwt.subject), request)

    @PutMapping("/portfolio/{projectId}")
    fun updatePortfolioProject(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: SavePortfolioProjectRequest
    ): PortfolioProjectResponse = portfolioService.update(UUID.fromString(jwt.subject), projectId, request)

    @DeleteMapping("/portfolio/{projectId}")
    fun deletePortfolioProject(@AuthenticationPrincipal jwt: Jwt, @PathVariable projectId: UUID) =
        portfolioService.delete(UUID.fromString(jwt.subject), projectId)

    @PutMapping("/portfolio/order")
    fun reorderPortfolioProjects(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ReorderPortfolioProjectsRequest
    ): List<PortfolioProjectResponse> = portfolioService.reorder(UUID.fromString(jwt.subject), request)

    @PostMapping("/portfolio/{projectId}/images", consumes = ["multipart/form-data"])
    fun uploadPortfolioImage(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @RequestPart("file") file: MultipartFile
    ): PortfolioImageResponse = portfolioService.uploadImage(UUID.fromString(jwt.subject), projectId, file)

    @DeleteMapping("/portfolio/images/{imageId}")
    fun deletePortfolioImage(@AuthenticationPrincipal jwt: Jwt, @PathVariable imageId: UUID) =
        portfolioService.deleteImage(UUID.fromString(jwt.subject), imageId)
}
