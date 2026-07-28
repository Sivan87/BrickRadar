package com.sivan.brickradar.model

import com.squareup.moshi.Json

// Rått svar från GET https://api.github.com/repos/Sivan87/BrickRadar/releases/latest
// (publikt repo sedan issue #1 i Sivan87/BrickRadar — inget API-nyckel-krav längre).
// Bara fälten appen faktiskt använder är med; Moshi ignorerar resten av GitHubs svar.
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "body") val body: String?,
    @Json(name = "assets") val assets: List<GitHubReleaseAsset>,
)

data class GitHubReleaseAsset(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
)
