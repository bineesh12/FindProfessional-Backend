package com.findprofessional.marketplace.service

import java.util.UUID

data class MarketplaceServiceResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val shortDescription: String,
    val iconKey: String,
    val imageUrl: String?,
    val categoryId: UUID,
    val categoryName: String
)

data class NearbyServiceResponse(
    val service: MarketplaceServiceResponse,
    val distanceKm: Double
)

fun MarketplaceService.toResponse() = MarketplaceServiceResponse(
    id = id,
    code = code,
    name = name,
    shortDescription = shortDescription,
    iconKey = iconKey,
    imageUrl = imageUrl,
    categoryId = category.id,
    categoryName = category.name
)
