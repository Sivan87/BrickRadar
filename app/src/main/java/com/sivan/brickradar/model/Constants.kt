package com.sivan.brickradar.model

// Union av Fas 4-kickoffens föreslagna lista och de valutasymboler webb-UI:t
// redan känner igen (mould-king-tracker/static/app.js: currencySymbol-mappen
// har EUR/USD/SEK/GBP) — GBP tillagd, CNY behållen från kickoff-listan.
val CURRENCIES = listOf("SEK", "EUR", "USD", "GBP", "CNY")

// Nyckeln _apply_model_fields (api.py) tolkar som "ingen kategori" (NULL i
// databasen) snarare än en riktig lagrad kategori — delad mellan detaljvyns
// redigeringsläge (Fas 3) och "Lägg till modell" (Fas 6).
const val UNCATEGORIZED_KEY = "ovrigt"

// Samma lista som datalist-förslagen i mould-king-tracker/templates/index.html
// ("+ Ny modell"-dialogen), plus "Generic" — det fria textvärde webbens
// modell/app.py (_is_manual_only_source) känner igen som "inget riktigt
// märke", satt av användaren för MOC-liknande set där ett ev. ifyllt
// modellnummer ändå inte motsvarar någon riktig butikskatalog.
val KNOWN_BRANDS = listOf(
    "Mould King", "Reobrix", "CaDA", "Panlos", "JieStar", "Mork",
    "Keeppley", "Sembo", "Pantasy", "TGL", "LEGO", "Generic",
)

data class CountryOption(val code: String, val displayName: String, val flagEmoji: String)

val COUNTRIES = listOf(
    CountryOption("CN", "Kina", "🇨🇳"),
    CountryOption("SE", "Sverige", "🇸🇪"),
    CountryOption("EU", "EU", "🇪🇺"),
    CountryOption("US", "USA", "🇺🇸"),
    CountryOption("GB", "Storbritannien", "🇬🇧"),
    CountryOption("DE", "Tyskland", "🇩🇪"),
)

fun flagForWarehouse(code: String?): String? = COUNTRIES.firstOrNull { it.code == code }?.flagEmoji
