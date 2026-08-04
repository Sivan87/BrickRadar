package com.sivan.brickradar.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private val apiKeyInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", ApiConfig.API_KEY)
            .build()
        chain.proceed(request)
    }

    // Skriver om schema/host/port på varje request till den adress BackendResolver senast
    // racade fram (se den filen och issue #19 i Sivan87/BrickRadar). Retrofits egen
    // baseUrl(...) nedan sätts bara en gång vid appstart och går inte att byta efteråt utan
    // att bygga om hela Retrofit-instansen - den här interceptorn är hur adressvalet
    // faktiskt blir dynamiskt per request istället.
    private val dynamicBaseUrlInterceptor = Interceptor { chain ->
        val original = chain.request()
        val target = BackendResolver.currentBaseUrl().toHttpUrl()
        val newUrl = original.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        chain.proceed(original.newBuilder().url(newUrl).build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // Ingen apiKeyInterceptor här — GitHub Releases är en publik, oautentiserad
    // endpoint sedan repot gjordes publikt (issue #1), och X-API-Key hör bara
    // hemma på anrop mot vår egen server.
    private val gitHubOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Placeholder-baseUrl - dynamicBaseUrlInterceptor ovan skriver alltid om schema/host/
    // port till den racade adressen innan requesten faktiskt skickas, så denna används bara
    // för Retrofits egen path-uppbyggnad, aldrig som den slutgiltiga anropsadressen.
    val api: BrickRadarApi = Retrofit.Builder()
        .baseUrl(ApiConfig.TAILSCALE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BrickRadarApi::class.java)

    val githubApi: GitHubReleasesApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(gitHubOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubReleasesApi::class.java)
}
