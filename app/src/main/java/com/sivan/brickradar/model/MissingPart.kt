package com.sivan.brickradar.model

import com.squareup.moshi.Json

// part_num-baserade köplänkar (ingen färgmappning i denna version) — se
// services/rebrickable_client.py: buy_links i mould-king-tracker. Null för
// bägge fälten om delen saknar part_num (t.ex. en manuell rad utan nummer).
data class BuyLinks(
    val bricklink: String?,
    val brickowl: String?,
)

data class MissingPart(
    val id: Int,
    @Json(name = "model_id") val modelId: Int,
    // "manual" (fritext, fungerar utan Rebrickable-konfiguration) eller
    // "rebrickable" (synkad, redigering av "found" anropar Rebrickable direkt).
    val origin: String,
    @Json(name = "rebrickable_inv_part_id") val rebrickableInvPartId: Int?,
    @Json(name = "part_num") val partNum: String?,
    val name: String,
    @Json(name = "color_name") val colorName: String?,
    @Json(name = "color_rgb") val colorRgb: String?,
    val quantity: Int,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "source_note") val sourceNote: String?,
    val found: Int,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "buy_links") val buyLinks: BuyLinks?,
) {
    val isFound: Boolean get() = found != 0
    val isFromRebrickable: Boolean get() = origin == "rebrickable"
}

data class MissingPartsResponse(
    val parts: List<MissingPart>,
    val total: Int,
    @Json(name = "found_count") val foundCount: Int,
    @Json(name = "synced_at") val syncedAt: String?,
    @Json(name = "rebrickable_set_num") val rebrickableSetNum: String?,
    @Json(name = "rebrickable_configured") val rebrickableConfigured: Boolean,
    val inactivity: MissingPartsInactivity?,
)
