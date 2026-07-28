package com.sivan.brickradar.model

import com.squareup.moshi.Json

// Speglar svaret från GET /api/brick4/search-by-number (api.py:
// api_brick4_search_by_number -> scraper.brick4_search_by_number). En rad per
// MÄRKE som har en exakt modellnummerträff hos Brick4 (inte en rad per set) —
// namn/bild/delantal ingår INTE i detta svar, bara det som behövs för att
// disambiguera märket. Namn/bild/delantal fylls i av samma asynkrona
// bakgrundshämtning (app.initial_fetch) som redan körs för alla nya modeller
// med känt modellnummer, precis som webbens "+ Ny modell"-formulär.
data class Brick4SearchResult(
    @Json(name = "brand_id") val brandId: String,
    @Json(name = "brand_name") val brandName: String,
    @Json(name = "item_id") val itemId: String,
    @Json(name = "title2url") val title2url: String,
    @Json(name = "setnumber_id") val setnumberId: String,
)
