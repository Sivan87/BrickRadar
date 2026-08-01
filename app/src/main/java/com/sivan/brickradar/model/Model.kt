package com.sivan.brickradar.model

import com.squareup.moshi.Json

data class Model(
    val id: Int,
    @Json(name = "model_number") val modelNumber: String,
    val name: String?,
    // DB-kolumnen är "TEXT DEFAULT 'Mould King'" utan NOT NULL, så äldre
    // poster kan sakna värde helt.
    val brand: String?,
    val status: String,
    @Json(name = "target_price") val targetPrice: Double?,
    val notes: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "piece_count") val pieceCount: Int?,
    val category: String?,
    val categories: String?,
    @Json(name = "is_official") val isOfficial: Int,
    @Json(name = "brickset_set_id") val bricksetSetId: String?,
    @Json(name = "official_piece_count") val officialPieceCount: Int?,
    @Json(name = "official_release_date") val officialReleaseDate: String?,
    @Json(name = "official_image_url") val officialImageUrl: String?,
    @Json(name = "official_rrp") val officialRrp: Double?,
    @Json(name = "official_rrp_currency") val officialRrpCurrency: String?,
    val ean: String?,
    @Json(name = "info_source_url") val infoSourceUrl: String?,
    @Json(name = "info_updated_at") val infoUpdatedAt: String?,
    @Json(name = "purchase_date") val purchaseDate: String?,
    @Json(name = "purchase_price") val purchasePrice: Double?,
    @Json(name = "purchase_source") val purchaseSource: String?,
    @Json(name = "release_year") val releaseYear: Int?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "best_kr_per_piece") val bestKrPerPiece: Double?,
    @Json(name = "best_value_rating") val bestValueRating: String?,
    @Json(name = "price_trend") val priceTrend: PriceTrend?,
    val prices: List<Source> = emptyList(),
    // Issue #17 (mirroring mould-king-tracker issue #5) — byggstatus/saknade
    // delar/eget foto/ordernummer/kvitton, bara meningsfulla när status =
    // "owned" (se api.py: _apply_model_fields, samma regel kontrolleras
    // server-side). build_status är NULL tills användaren valt ett värde.
    @Json(name = "build_status") val buildStatus: String? = null,
    @Json(name = "build_completed_at") val buildCompletedAt: String? = null,
    @Json(name = "own_photo_url") val ownPhotoUrl: String? = null,
    @Json(name = "order_number") val orderNumber: String? = null,
    @Json(name = "rebrickable_set_num") val rebrickableSetNum: String? = null,
    @Json(name = "missing_parts_synced_at") val missingPartsSyncedAt: String? = null,
    @Json(name = "missing_parts_status_since") val missingPartsStatusSince: String? = null,
    @Json(name = "missing_parts_inactivity") val missingPartsInactivity: MissingPartsInactivity? = null,
) {
    val isOfficialSet: Boolean get() = isOfficial != 0
}

// Delas av Model.missingPartsInactivity och MissingPartsResponse.inactivity
// (samma form returneras på båda ställena, se api.py: _missing_parts_inactivity).
data class MissingPartsInactivity(
    val days: Int,
    val stale: Boolean,
)

data class PriceTrend(
    val pct: Double?,
    @Json(name = "days_since_change") val daysSinceChange: Int?,
    @Json(name = "is_all_time_low") val isAllTimeLow: Boolean?,
)

data class Source(
    val id: Int,
    @Json(name = "model_id") val modelId: Int,
    val source: String,
    val price: Double?,
    // DB-kolumnen är "TEXT DEFAULT 'EUR'" utan NOT NULL, så äldre poster kan
    // sakna värde helt.
    val currency: String?,
    val url: String?,
    @Json(name = "in_stock") val inStock: Int?,
    @Json(name = "in_stock_override") val inStockOverride: Boolean,
    @Json(name = "is_manual") val isManual: Int,
    val warehouse: String?,
    @Json(name = "warehouse_override") val warehouseOverride: Boolean,
    @Json(name = "delivery_estimate") val deliveryEstimate: String?,
    @Json(name = "scraped_at") val scrapedAt: String,
    @Json(name = "shipping_sek") val shippingSek: Double?,
    @Json(name = "shipping_source") val shippingSource: String?,
    @Json(name = "shipping_override_amount") val shippingOverrideAmount: Double?,
    @Json(name = "shipping_override_currency") val shippingOverrideCurrency: String?,
    @Json(name = "total_price_sek") val totalPriceSek: Double?,
    @Json(name = "kr_per_piece") val krPerPiece: Double?,
    @Json(name = "value_rating") val valueRating: String?,
    // _flag_suspicious_prices (api.py) hoppar över att sätta detta fält helt
    // om modellen har färre än 2 priser med känt kr_per_piece att jämföra
    // mot, eller om det lägsta är <= 0 — då saknas nyckeln i JSON:en helt.
    // Frånvaro betyder "kunde inte avgöras", vilket i UI:t ska visas som
    // "inte flaggad" precis som false.
    val suspicious: Boolean = false,
) {
    val isManualEntry: Boolean get() = isManual != 0
}
