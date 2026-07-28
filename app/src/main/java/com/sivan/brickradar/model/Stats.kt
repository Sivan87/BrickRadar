package com.sivan.brickradar.model

import com.squareup.moshi.Json

// GET /api/stats (api.py: api_stats) — hela svaret modelleras nu (Fas 11,
// se CLAUDE.md/issue #6 i Sivan87/BrickRadar: Statistik-fliken ska ha full
// paritet med webbverktygets egen statistiksida, inte bara Klon-/LEGO-snittet
// som var version 1, se Fas 10). Fältnamn/former verifierade direkt mot
// api.py:api_stats/_category_breakdown, inte bara antagna.
data class StatsResponse(
    val counts: StatusCounts?,
    @Json(name = "total_pieces_owned") val totalPiecesOwned: Int?,
    @Json(name = "estimated_collection_value_sek") val estimatedCollectionValueSek: Double?,
    @Json(name = "avg_kr_per_piece_owned") val avgKrPerPieceOwned: Double?,
    @Json(name = "avg_kr_per_piece_clone_all") val avgKrPerPieceCloneAll: Double?,
    @Json(name = "avg_kr_per_piece_lego_all") val avgKrPerPieceLegoAll: Double?,
    @Json(name = "value_distribution_watching") val valueDistributionWatching: ValueDistribution?,
    @Json(name = "best_deals") val bestDeals: List<StatsModelRef> = emptyList(),
    @Json(name = "brand_breakdown") val brandBreakdown: List<BrandBreakdown> = emptyList(),
    @Json(name = "category_breakdown") val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
)

data class StatusCounts(
    val new: Int = 0,
    val watching: Int = 0,
    val owned: Int = 0,
    val rejected: Int = 0,
)

// value_distribution_watching — antal bevakade modeller med känt pris per
// kr/del-tröskeltier (samma 5 tiers som util/ValueRating.kt).
data class ValueDistribution(
    val cyan: Int = 0,
    val green: Int = 0,
    val yellow: Int = 0,
    val orange: Int = 0,
    val red: Int = 0,
) {
    val total: Int get() = cyan + green + yellow + orange + red
}

// Delad form för best_deals-poster OCH category_breakdown[].best (samma dict-
// form i api.py på båda ställena).
data class StatsModelRef(
    val id: Int,
    val brand: String?,
    @Json(name = "model_number") val modelNumber: String?,
    val name: String?,
    @Json(name = "best_kr_per_piece") val bestKrPerPiece: Double,
    @Json(name = "best_value_rating") val bestValueRating: String?,
)

data class BrandBreakdown(
    val brand: String,
    val count: Int,
    @Json(name = "avg_kr_per_piece") val avgKrPerPiece: Double?,
)

data class CategoryBreakdown(
    val category: String,
    val label: String,
    val count: Int,
    @Json(name = "avg_kr_per_piece") val avgKrPerPiece: Double?,
    val best: StatsModelRef?,
)
