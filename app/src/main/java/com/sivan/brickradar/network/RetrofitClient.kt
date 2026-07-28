package com.sivan.brickradar.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: BrickRadarApi = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BrickRadarApi::class.java)
}
