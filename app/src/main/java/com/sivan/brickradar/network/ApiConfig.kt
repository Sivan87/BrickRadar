package com.sivan.brickradar.network

import com.sivan.brickradar.BuildConfig

object ApiConfig {
    // Värdena kommer nu från BuildConfig (genererat av Gradle från gradle.properties
    // lokalt, eller -P i CI, se app/build.gradle.kts) - inte hårdkodat i källkoden,
    // se CLAUDE.md "Serverkonfiguration" och issue #2 i Sivan87/BrickRadar.
    // Ändras servern av dator/nätverk måste API_BASE_URL uppdateras i gradle.properties.
    val TAILSCALE_URL: String = BuildConfig.API_BASE_URL
    val LOCAL_URL: String = BuildConfig.API_BASE_URL_LOCAL
    val API_KEY: String = BuildConfig.API_KEY

    // Racead/cachad av BackendResolver (se den filen och issue #19 i Sivan87/BrickRadar) -
    // BASE_URL är alltså inte längre ett statiskt BuildConfig-värde utan speglar vilken av
    // TAILSCALE_URL/LOCAL_URL som senast svarade snabbast. authenticatedUrl nedan (Coil-
    // bilder, kvittovisning) och RetrofitClients OkHttp-interceptor läser båda samma adress.
    val BASE_URL: String
        get() = BackendResolver.currentBaseUrl()

    // För <img>-liknande anrop som inte kan sätta en X-API-Key-header (Coils
    // AsyncImage, eller en Intent(ACTION_VIEW) mot ett kvitto) — _require_api_key
    // (api.py, mould-king-tracker) accepterar nyckeln som ?api_key=-query-
    // parameter på ALLA /api/*-routes, inte bara bild-/foto-endpointerna
    // (samma mekanism static/app.js:s modellImageUrl/buildPhotoUrl redan använder).
    fun authenticatedUrl(path: String): String =
        "${BASE_URL.trimEnd('/')}/$path?api_key=$API_KEY"
}
