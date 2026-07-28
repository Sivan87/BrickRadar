package com.sivan.brickradar.model

import com.squareup.moshi.Json

// GET /api/stats (api.py: api_stats) returnerar mycket mer än detta, men
// appen använder bara referenspunkterna för detaljvyns värde-skala
// ("Klonsnitt"/"LEGO-snitt", se ui-redesign-anteckningar/design-doc-rond t6d)
// samt statusräknarna som redan visas i mockupernas filterchips (Alla 12/
// Bevakar 5/Äger 3) — Moshi ignorerar okända fält, så resten av svaret
// behöver inte modelleras.
data class StatsResponse(
    val counts: StatusCounts?,
    @Json(name = "avg_kr_per_piece_clone_all") val avgKrPerPieceCloneAll: Double?,
    @Json(name = "avg_kr_per_piece_lego_all") val avgKrPerPieceLegoAll: Double?,
)

data class StatusCounts(
    val new: Int = 0,
    val watching: Int = 0,
    val owned: Int = 0,
    val rejected: Int = 0,
)
