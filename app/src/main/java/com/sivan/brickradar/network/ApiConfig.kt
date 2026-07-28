package com.sivan.brickradar.network

import com.sivan.brickradar.BuildConfig

object ApiConfig {
    // Värdena kommer nu från BuildConfig (genererat av Gradle från gradle.properties
    // lokalt, eller -P i CI, se app/build.gradle.kts) - inte hårdkodat i källkoden,
    // se CLAUDE.md "Serverkonfiguration" och issue #2 i Sivan87/BrickRadar.
    // Ändras servern av dator/nätverk måste API_BASE_URL uppdateras i gradle.properties.
    val BASE_URL: String = BuildConfig.API_BASE_URL
    val API_KEY: String = BuildConfig.API_KEY
}
