import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseStoreFile = project.findProperty("RELEASE_STORE_FILE") as String?
val releaseStorePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
val releaseKeyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
val releaseKeyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?

// Serverns bas-URL/API-nyckel - lokalt i gradle.properties (gitignorad) eller
// via -P i CI (release.yml), aldrig hårdkodat i källkoden (se CLAUDE.md
// "Serverkonfiguration" och issue #2 i Sivan87/BrickRadar).
val apiBaseUrl = project.findProperty("API_BASE_URL") as String?
    ?: throw GradleException("API_BASE_URL saknas - se CLAUDE.md \"Serverkonfiguration\"")
val apiKey = project.findProperty("API_KEY") as String?
    ?: throw GradleException("API_KEY saknas - se CLAUDE.md \"Serverkonfiguration\"")

// Lokal LAN-fallback (issue #19 i Sivan87/BrickRadar) - till skillnad från API_BASE_URL/
// API_KEY ovan kastar den INTE ett fel om den saknas: adressen är en privat LAN-IP (ingen
// hemlighet, oåtkomlig utanför hemmanätverket) med ett känt, stabilt default-värde, så varken
// lokala byggen eller CI (release.yml, ingen ny secret) behöver sätta den explicit.
val apiBaseUrlLocal = project.findProperty("API_BASE_URL_LOCAL") as String?
    ?: "http://192.168.1.142:5000"

// version.properties är den enda källan till sanning för versionCode/versionName
// (se CLAUDE.md, "Release build") — höjs automatiskt här vid assembleRelease
// istället för att redigeras manuellt.
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}
val storedVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("Release") }
// GitHub Actions sätter CI=true automatiskt. Release-workflowet (.github/workflows/release.yml)
// höjer och committar version.properties själv INNAN gradlew körs där, så denna auto-höjning
// måste stå över i CI — annars dubbelhöjs versionen (en gång av workflowet, en gång här).
val isCI = System.getenv("CI") == "true"

val appVersionCode = if (isReleaseBuild && !isCI) storedVersionCode + 1 else storedVersionCode
val appVersionName = if (isReleaseBuild && !isCI) "1.$appVersionCode" else versionProps.getProperty("VERSION_NAME", "1.0")

if (isReleaseBuild && !isCI) {
    // Skriver rått istället för Properties.store(), som annars alltid
    // lägger till en tidsstämpel-kommentarsrad högst upp i filen.
    versionPropsFile.writeText("VERSION_CODE=$appVersionCode\nVERSION_NAME=$appVersionName\n")
}

android {
    namespace = "com.sivan.brickradar"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        compose = true
        // Behövs uttryckligen sedan AGP 8 (BuildConfig-generering är
        // avstängd som default) — UpdateViewModel jämför BuildConfig.VERSION_CODE
        // mot servern.
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.sivan.brickradar"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "API_BASE_URL_LOCAL", "\"$apiBaseUrlLocal\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Inte med i kickoff-dokumentets beroendelista, men behövs för att visa
    // modellbilder från image_url i listan/detaljvyn (se ModelListScreen/
    // ModelDetailScreen) — standardvalet för bildladdning i Compose.
    implementation("io.coil-kt:coil-compose:2.7.0")
}