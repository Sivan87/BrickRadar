package com.sivan.brickradar.model

import com.squareup.moshi.Json

// Speglar svaret från GET /api/app-version (mould-king-tracker: api.py,
// api_app_version) — läser version.json på servern, skrivet av
// publish-update.py. downloadUrl byggs server-side från request.host_url,
// pekar alltså alltid på rätt LAN-IP oavsett vad ApiConfig.BASE_URL är satt
// till just nu.
data class AppVersionResponse(
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "versionName") val versionName: String,
    @Json(name = "releaseNotes") val releaseNotes: String,
    @Json(name = "downloadUrl") val downloadUrl: String,
)
