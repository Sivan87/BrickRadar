package com.sivan.brickradar.model

import com.squareup.moshi.Json

// Speglar body:n POST /api/models (api.py: api_add_model) förväntar sig.
// OBS två bekräftade avvikelser från fas6-kickoffdokumentet, verifierade mot
// api.py innan detta skrevs:
// 1) "brand" är OBLIGATORISKT server-side ("brand krävs", 400 annars) — inte
//    med i kickoff-dokumentets fältlista alls. Se AddModelScreen/KNOWN_BRANDS.
// 2) "category" skickas INTE med här — servern ignorerar helt ett ev. sådant
//    fält i denna endpoint och sätter alltid category = suggest_category(name)
//    (enkel nyckelordsgissning). ModelRepository.addModel gör därför ett
//    uppföljande PUT /models/{id} (samma väg som redigeringsläget i Fas 3)
//    om användaren valt en annan kategori än auto-gissningen.
// name/piece_count är nullable (Fas 7) — servern accepterar båda tomma
// (data.get("name") / _parse_piece_count(None) -> None) och fyller dem i
// senare via initial_fetch. Det manuella flödet (Fas 6) skickar dem alltid
// ifyllda eftersom AddModelScreen kräver dem där; sökflödet (Fas 7) skickar
// dem tomma om användaren inte själv rättat dem i bekräftelseformuläret,
// exakt som webbens "+ Ny modell"-formulär redan gör.
data class AddModelRequest(
    @Json(name = "model_number") val modelNumber: String,
    val brand: String,
    val name: String?,
    @Json(name = "piece_count") val pieceCount: Int?,
    val status: String,
    @Json(name = "image_url") val imageUrl: String?,
)
