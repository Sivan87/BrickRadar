package com.sivan.brickradar.model

import com.squareup.moshi.Json

data class StatusUpdateRequest(
    val status: String,
)

data class ModelUpdateRequest(
    val name: String,
    @Json(name = "piece_count") val pieceCount: Int,
    val category: String,
    // Anteckningsfältet är oberoende av byggstatus/status (issue #5, del 2 i
    // mould-king-tracker) — redigeras i samma formulär som namn/delantal/
    // kategori eftersom det redan är en del av _apply_model_fields' delade
    // fältvalidering och inte behöver en egen dedikerad endpoint.
    val notes: String,
)

// PUT/PATCH /models/{id} delar samma fältvalidering (_apply_model_fields,
// api.py) och accepterar ett DELVIS fältset — bara de nycklar som finns i
// body:n uppdateras. Används av ModelRepository.addModel för att sätta
// kategorin efter skapande UTAN att skicka med name/piece_count (som kan
// vara null i sökflödet, Fas 7, innan bakgrundshämtningen fyllt dem i).
data class CategoryUpdateRequest(
    val category: String,
)

// Issue #17 (mirroring mould-king-tracker issue #5) — smala, enfälts
// partial-uppdateringar mot samma PUT /models/{id}, samma mönster som
// CategoryUpdateRequest ovan (bara det egna fältet skickas, resten av
// modellen lämnas orörd av _apply_model_fields).
data class OrderNumberUpdateRequest(
    @Json(name = "order_number") val orderNumber: String,
)

data class RebrickableSetNumUpdateRequest(
    @Json(name = "rebrickable_set_num") val rebrickableSetNum: String,
)

// PATCH /models/{id}/build-status (dedikerad endpoint, api.py:
// api_update_build_status) — buildCompletedAt är valfri (Moshi utelämnar
// null-fält vid serialisering som standard, se RetrofitClient/moshi), servern
// sätter den automatiskt till "nu" när build_status blir "byggd" om anropet
// inte redan skickar med ett eget värde.
data class BuildStatusUpdateRequest(
    @Json(name = "build_status") val buildStatus: String?,
    @Json(name = "build_completed_at") val buildCompletedAt: String? = null,
)
