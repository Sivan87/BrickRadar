package com.sivan.brickradar.model

import com.squareup.moshi.Json

// Appens interna representation av "senaste tillgängliga version" — byggs i
// ModelRepository.getAppVersion() av GitHubReleaseResponse (GET
// /repos/Sivan87/BrickRadar/releases/latest), inte längre av vår egen server
// (se issue #1 i Sivan87/BrickRadar). downloadUrl pekar direkt på GitHubs
// browser_download_url för release-APK:n.
data class AppVersionResponse(
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "versionName") val versionName: String,
    @Json(name = "releaseNotes") val releaseNotes: String,
    @Json(name = "downloadUrl") val downloadUrl: String,
)
