package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferAttachmentRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.professional.PortfolioImageRepository
import com.findprofessional.marketplace.professional.PortfolioProjectRepository
import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

@Service
class CustomerRequestQueryService(
    private val authorization: CustomerAuthorizationService,
    private val requests: CustomerRequestRepository,
    private val locations: RequestLocationRepository,
    private val answers: RequestAnswerRepository,
    private val offers: ProfessionalOfferRepository,
    private val profiles: ProfessionalProfileRepository,
    private val portfolioProjects: PortfolioProjectRepository,
    private val portfolioImages: PortfolioImageRepository,
    private val offerAttachments: ProfessionalOfferAttachmentRepository,
    private val storageProperties: PortfolioStorageProperties
) {
    @Transactional(readOnly = true)
    fun list(
        userId: UUID,
        filter: CustomerRequestFilter,
        page: Int,
        size: Int
    ): CustomerRequestListResponse {
        authorization.requireCustomer(userId)
        if (page < 0 || size !in 1..MaximumPageSize) {
            throw RequestException("Invalid request page", "INVALID_PAGE")
        }

        val activeCount = requests.countByCustomerIdAndStatusIn(userId, ActiveStatuses)
        val completedCount = requests.countByCustomerIdAndStatusIn(userId, CompletedStatuses)
        val counts = CustomerRequestCountsResponse(
            all = activeCount + completedCount,
            active = activeCount,
            completed = completedCount
        )

        val requestPage = requests.findAllByCustomerIdAndStatusInOrderByCreatedAtDesc(
            userId,
            filter.statuses,
            PageRequest.of(page, size)
        )
        val pageRequests = requestPage.content
        if (pageRequests.isEmpty()) {
            return CustomerRequestListResponse(
                counts,
                emptyList(),
                page,
                size,
                requestPage.totalElements,
                requestPage.totalPages
            )
        }

        val requestIds = pageRequests.map { it.id }
        val locationByRequestId = locations.findAllByRequestIdIn(requestIds)
            .groupBy { it.request.id }
            .mapValues { (_, values) -> values.preferredLocation() }
        val answerBySessionId = answers.findAllBySessionIdIn(pageRequests.map { it.session.id })
            .groupBy { it.session.id }
        val offerCountByRequestId = offers.countByRequestIdsAndStatus(
            requestIds,
            ProfessionalOfferStatus.SUBMITTED
        ).associate { it.requestId to it.offerCount }

        return CustomerRequestListResponse(
            counts = counts,
            requests = pageRequests.map { request ->
                val offerCount = offerCountByRequestId[request.id] ?: 0
                request.toListItem(
                    locationByRequestId[request.id],
                    answerBySessionId[request.session.id].orEmpty(),
                    offerCount
                )
            },
            page = page,
            size = size,
            totalCount = requestPage.totalElements,
            totalPages = requestPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun offers(
        userId: UUID,
        requestId: UUID,
        sort: CustomerOfferSort
    ): CustomerRequestOffersResponse {
        authorization.requireCustomer(userId)
        val request = requireOwnedRequest(userId, requestId)
        val submittedOffers = offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(
            requestId,
            ProfessionalOfferStatus.SUBMITTED
        )
        val professionalById = professionalSummaries(submittedOffers.map { it.professionalUserId })
        return CustomerRequestOffersResponse(
            request = request.toOfferRequest(submittedOffers.size.toLong()),
            offers = submittedOffers.sorted(sort).map { offer ->
                offer.toCustomerOffer(checkNotNull(professionalById[offer.professionalUserId]))
            }
        )
    }

    @Transactional(readOnly = true)
    fun offerDetails(userId: UUID, requestId: UUID, offerId: UUID): CustomerOfferDetailsResponse {
        authorization.requireCustomer(userId)
        val request = requireOwnedRequest(userId, requestId)
        val offer = offers.findByIdAndRequestIdAndStatus(offerId, requestId, ProfessionalOfferStatus.SUBMITTED)
            .orElseThrow {
                RequestException(
                    "Offer was not found",
                    "OFFER_NOT_FOUND",
                    org.springframework.http.HttpStatus.NOT_FOUND
                )
            }
        val professional = checkNotNull(
            professionalSummaries(listOf(offer.professionalUserId))[offer.professionalUserId]
        )
        return CustomerOfferDetailsResponse(
            request = request.toOfferRequest(
                offers.countByRequestIdAndStatus(requestId, ProfessionalOfferStatus.SUBMITTED)
            ),
            offer = offer.toCustomerOffer(professional),
            attachments = offerAttachments.findAllByOfferIdOrderByCreatedAtAsc(offer.id).map { attachment ->
                CustomerOfferAttachmentResponse(
                    id = attachment.id,
                    filename = attachment.originalFilename,
                    contentType = attachment.contentType,
                    sizeBytes = attachment.sizeBytes,
                    url = publicUrl(attachment.storageKey)
                )
            }
        )
    }

    @Transactional
    fun update(
        userId: UUID,
        requestId: UUID,
        input: UpdateCustomerRequestRequest
    ): CustomerRequestUpdateResponse {
        authorization.requireCustomer(userId)
        val request = requireOwnedRequest(userId, requestId)
        if (request.status != CustomerRequestStatus.PUBLISHED) {
            throw RequestException(
                "Only active requests can be edited",
                "REQUEST_NOT_EDITABLE",
                HttpStatus.CONFLICT
            )
        }
        if (offers.countByRequestIdAndStatus(requestId, ProfessionalOfferStatus.SUBMITTED) > 0) {
            throw RequestException(
                "Requests with submitted offers cannot be edited",
                "REQUEST_HAS_OFFERS",
                HttpStatus.CONFLICT
            )
        }
        val title = input.title.trim()
        val description = input.description.trim()
        if (title.length !in MinimumTitleLength..MaximumTitleLength) {
            throw RequestException(
                "Request title must be between $MinimumTitleLength and $MaximumTitleLength characters",
                "INVALID_REQUEST_TITLE"
            )
        }
        if (description.length !in MinimumDescriptionLength..MaximumDescriptionLength) {
            throw RequestException(
                "Request description must be between $MinimumDescriptionLength and $MaximumDescriptionLength characters",
                "INVALID_REQUEST_DESCRIPTION"
            )
        }
        request.title = title
        request.description = description
        request.updatedAt = Instant.now()
        return CustomerRequestUpdateResponse(
            id = request.id,
            title = request.title,
            description = request.description,
            updatedAt = request.updatedAt
        )
    }

    private fun CustomerRequest.toListItem(
        location: RequestLocation?,
        requestAnswers: List<RequestAnswer>,
        offerCount: Long
    ) = CustomerRequestListItemResponse(
        id = id,
        title = title,
        description = description,
        categoryName = category.name,
        serviceName = service.name,
        serviceIconKey = service.iconKey,
        status = status,
        location = location?.let { CustomerRequestLocationResponse(it.municipality, it.postalCode) },
        budget = requestAnswers.firstOrNull { it.question.type == QuestionType.MONEY }
            ?.value
            ?.toBudget(),
        submittedOfferCount = offerCount,
        canEdit = offerCount == 0L,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun String.toBudget(): CustomerRequestBudgetResponse? {
        val value = trim()
        val match = MoneyValuePattern.matchEntire(value)
        return if (match == null) {
            value.toBigDecimalOrNull()?.let { CustomerRequestBudgetResponse(it.toPlainString(), null) }
        } else {
            CustomerRequestBudgetResponse(match.groupValues[1], match.groupValues[2])
        }
    }

    private fun List<RequestLocation>.preferredLocation(): RequestLocation? =
        firstOrNull { it.kind == RequestLocationKind.SERVICE }
            ?: firstOrNull { it.kind == RequestLocationKind.PROJECT }
            ?: firstOrNull { it.kind == RequestLocationKind.PICKUP }
            ?: firstOrNull()

    private fun requireOwnedRequest(userId: UUID, requestId: UUID) =
        requests.findByIdAndCustomerId(requestId, userId).orElseThrow {
            RequestException(
                "Request was not found",
                "REQUEST_NOT_FOUND",
                org.springframework.http.HttpStatus.NOT_FOUND
            )
        }

    private fun CustomerRequest.toOfferRequest(submittedOfferCount: Long): CustomerOfferRequestResponse {
        val requestLocation = locations.findAllByRequestIdIn(listOf(id)).preferredLocation()
        val requestBudget = answers.findAllBySessionId(session.id)
            .firstOrNull { it.question.type == QuestionType.MONEY }
            ?.value
            ?.toBudget()
        return CustomerOfferRequestResponse(
            id = id,
            title = title,
            description = description,
            serviceName = service.name,
            location = requestLocation?.let {
                CustomerRequestLocationResponse(it.municipality, it.postalCode)
            },
            budget = requestBudget,
            submittedOfferCount = submittedOfferCount
        )
    }

    private fun professionalSummaries(userIds: Collection<UUID>): Map<UUID, CustomerOfferProfessionalResponse> {
        val distinctUserIds = userIds.distinct()
        if (distinctUserIds.isEmpty()) return emptyMap()
        val profileById = profiles.findAllById(distinctUserIds).associateBy { it.userId }
        val projects = portfolioProjects.findAllByProfessionalUserIdIn(distinctUserIds)
            .sortedWith(compareBy({ it.professionalUserId }, { it.displayOrder }, { it.createdAt }))
        val imageByProjectId = if (projects.isEmpty()) {
            emptyMap()
        } else {
            portfolioImages.findAllByProjectIdInOrderByDisplayOrderAscCreatedAtAsc(projects.map { it.id })
                .groupBy { it.project.id }
        }
        val thumbnailByProfessionalId = projects.groupBy { it.professionalUserId }.mapValues { (_, values) ->
            values.firstNotNullOfOrNull { project ->
                imageByProjectId[project.id]?.firstOrNull()?.storageKey
            }
        }
        return profileById.mapValues { (userId, profile) ->
            CustomerOfferProfessionalResponse(
                id = userId,
                businessName = profile.businessName,
                serviceArea = profile.serviceArea,
                experienceYears = profile.experienceYears,
                about = profile.about,
                portfolioImageUrl = thumbnailByProfessionalId[userId]?.let(::publicUrl)
            )
        }
    }

    private fun ProfessionalOffer.toCustomerOffer(professional: CustomerOfferProfessionalResponse) =
        CustomerOfferListItemResponse(
            id = id,
            professional = professional,
            amount = checkNotNull(amount).stripTrailingZeros().toPlainString(),
            currency = currency,
            message = message,
            estimatedDays = estimatedDays,
            availableStartDate = availableStartDate,
            scopeIncluded = scopeIncluded,
            scopeExcluded = scopeExcluded,
            submittedAt = updatedAt
        )

    private fun List<ProfessionalOffer>.sorted(sort: CustomerOfferSort): List<ProfessionalOffer> = when (sort) {
        CustomerOfferSort.LATEST -> sortedByDescending { it.updatedAt }
        CustomerOfferSort.PRICE_ASC -> sortedBy { it.amount }
        CustomerOfferSort.START_DATE_ASC -> sortedWith(
            compareBy<ProfessionalOffer> { it.availableStartDate == null }
                .thenBy { it.availableStartDate }
                .thenBy { it.amount }
        )
    }

    private fun publicUrl(storageKey: String) =
        "${storageProperties.publicPath.trimEnd('/')}/$storageKey"

    private companion object {
        const val MaximumPageSize = 50
        const val MinimumTitleLength = 5
        const val MaximumTitleLength = 180
        const val MinimumDescriptionLength = 20
        const val MaximumDescriptionLength = 4000
        val MoneyValuePattern = Regex("""^(\d+(?:\.\d+)?)\s+([A-Z]{3})$""")
        val ActiveStatuses = setOf(CustomerRequestStatus.PUBLISHED)
        val CompletedStatuses = setOf(CustomerRequestStatus.COMPLETED)
    }
}

private val CustomerRequestFilter.statuses: Set<CustomerRequestStatus>
    get() = when (this) {
        CustomerRequestFilter.ALL -> CustomerRequestStatus.entries.toSet()
        CustomerRequestFilter.ACTIVE -> setOf(CustomerRequestStatus.PUBLISHED)
        CustomerRequestFilter.COMPLETED -> setOf(CustomerRequestStatus.COMPLETED)
    }
