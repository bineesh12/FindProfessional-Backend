package com.findprofessional.marketplace.category

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val iconKey: String
)

fun ServiceCategory.toResponse() = CategoryResponse(id, code, name, iconKey)
