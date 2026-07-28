package com.sivan.brickradar.model

import com.squareup.moshi.Json

data class CategoriesResponse(
    val categories: List<Category>,
)

data class Category(
    val category: String,
    val label: String,
    val count: Int,
    @Json(name = "avg_kr_per_piece") val avgKrPerPiece: Double?,
    val best: CategoryBest?,
)

data class CategoryBest(
    val id: Int,
    @Json(name = "model_number") val modelNumber: String,
    // Mirrorar models.brand (se Model.kt) — nullable av samma skäl.
    val brand: String?,
    val name: String?,
    @Json(name = "best_kr_per_piece") val bestKrPerPiece: Double,
    @Json(name = "best_value_rating") val bestValueRating: String?,
)
