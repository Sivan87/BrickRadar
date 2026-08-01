package com.sivan.brickradar.model

import com.squareup.moshi.Json

data class Receipt(
    val id: Int,
    @Json(name = "model_id") val modelId: Int,
    val filename: String,
    @Json(name = "original_filename") val originalFilename: String?,
    @Json(name = "uploaded_at") val uploadedAt: String,
)
