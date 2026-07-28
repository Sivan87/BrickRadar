package com.sivan.brickradar.network

import com.sivan.brickradar.model.GitHubReleaseResponse
import retrofit2.http.GET

// Publikt repo (github.com/Sivan87/BrickRadar, se issue #1) — GitHub Releases
// kräver ingen autentisering för publika repon, så det här går via en egen
// Retrofit-instans (RetrofitClient.githubApi) utan X-API-Key-interceptorn och
// oberoende av ApiConfig.BASE_URL/hemma-WiFi.
interface GitHubReleasesApi {
    @GET("repos/Sivan87/BrickRadar/releases/latest")
    suspend fun getLatestRelease(): GitHubReleaseResponse
}
