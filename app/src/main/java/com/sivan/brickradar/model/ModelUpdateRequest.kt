package com.sivan.brickradar.model

import com.squareup.moshi.Json

data class StatusUpdateRequest(
    val status: String,
)

data class ModelUpdateRequest(
    val name: String,
    @Json(name = "piece_count") val pieceCount: Int,
    val category: String,
)

// PUT/PATCH /models/{id} delar samma fältvalidering (_apply_model_fields,
// api.py) och accepterar ett DELVIS fältset — bara de nycklar som finns i
// body:n uppdateras. Används av ModelRepository.addModel för att sätta
// kategorin efter skapande UTAN att skicka med name/piece_count (som kan
// vara null i sökflödet, Fas 7, innan bakgrundshämtningen fyllt dem i).
data class CategoryUpdateRequest(
    val category: String,
)
