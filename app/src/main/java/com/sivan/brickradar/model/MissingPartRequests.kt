package com.sivan.brickradar.model

import com.squareup.moshi.Json

// POST /models/{id}/missing-parts — manuell fallback-rad (fungerar utan
// Rebrickable-konfiguration, se api.py: api_add_missing_part). partNum/
// colorName/sourceNote är valfria (Moshi utelämnar null-fält vid
// serialisering som standard).
data class AddMissingPartRequest(
    val name: String,
    @Json(name = "part_num") val partNum: String? = null,
    @Json(name = "color_name") val colorName: String? = null,
    val quantity: Int = 1,
    @Json(name = "source_note") val sourceNote: String? = null,
)

// PATCH /models/{id}/missing-parts/{partId} — bara avbockning stöds från
// appen (inte fritextredigering av en redan tillagd rad, se issue-
// kravlistan: "sökbar, avbockningsbar", ingen redigeringskrav). En avbockad
// "rebrickable"-rad synkar omedelbart mot Rebrickables egen lost-parts-lista
// server-side (api.py: api_update_missing_part).
data class ToggleMissingPartFoundRequest(
    val found: Boolean,
)
