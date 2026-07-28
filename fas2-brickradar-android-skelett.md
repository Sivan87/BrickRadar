# Kickoff: BrickRadar Android – Fas 2: Grundskelett (Retrofit + navigation + list/detaljvy)

## Bakgrund
Flask-backend har nu ett JSON-API under `/api/` (klart och verifierat i separat session, se `mould-king-tracker/CLAUDE.md`). Skyddat med `X-API-Key`-header. Detta projekt (`C:\BrickRadarApp`) är ett tomt Compose-projekt som byggts och körts framgångsrikt (Hello World-nivå, se `MainActivity.kt`).

Målet med den här fasen: bygga grundstrukturen som resten av appen byggs vidare på — nätverkslager, navigation, och de två första riktiga skärmarna (lista + detalj), read-only (ingen redigering än, det kommer i senare faser).

**Viktigt om miljön:** Servern nås just nu ENBART på hemma-WiFi via lokal IP (t.ex. `http://192.168.X.X:5000`), ingen VPN ännu. Server-adress och API-nyckel ska vara **hårdkodade konstanter** i koden (inte en inställningsskärm) — enligt uttryckligt beslut, för enkelhetens skull i detta skede.

## Uppgifter

### 1. Lägg till dependencies i `app/build.gradle.kts`
Lägg till i `dependencies { ... }` (behåll det som redan finns från Fas 0):
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("androidx.navigation:navigation-compose:2.8.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
```
Uppdatera `gradle/libs.versions.toml` med motsvarande version-/library-poster om ni vill följa samma mönster som befintliga beroenden, annars är direkta strängar i `build.gradle.kts` okej för denna fas.

### 2. Nätverkskonfiguration för lokal HTTP (cleartext)
Android blockerar vanlig HTTP (icke-HTTPS) som standard sedan API 28. Eftersom servern körs på lokalt nätverk utan HTTPS behövs en network security config:
- Skapa `app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">DIN_SERVER_IP_HÄR</domain>
    </domain-config>
</network-security-config>
```
- Referera den i `AndroidManifest.xml`, i `<application>`-taggen: `android:networkSecurityConfig="@xml/network_security_config"`
- Lägg även till `<uses-permission android:name="android.permission.INTERNET" />` i manifestet (utanför `<application>`-taggen) om den inte redan finns

### 3. Konstanter för server + API-nyckel
Skapa `app/src/main/java/com/sivan/brickradar/network/ApiConfig.kt`:
```kotlin
object ApiConfig {
    const val BASE_URL = "http://DIN_SERVER_IP_HÄR:5000/"
    const val API_KEY = "DIN_API_NYCKEL_HÄR"
}
```
Fråga mig (användaren) om exakt IP och nyckel innan detta fylls i — hämta nyckeln från `.env`-filen i `mould-king-tracker`-projektet.

### 4. Datamodeller
Skapa `app/src/main/java/com/sivan/brickradar/model/` med Kotlin data classes som speglar JSON-strukturen från API:t: `Model`, `Source`, `Category` etc. Basera fälten på vad `/api/models` och `/api/models/<id>` faktiskt returnerar — inspektera live-svar (via curl eller webbläsaren) om strukturen inte är helt uppenbar från kickoff-filen för Fas 1.

### 5. Retrofit-klient + API-interface
- `network/BrickRadarApi.kt` — Retrofit-interface med minst:
  - `GET api/models` (med valfria query-parametrar för status/kategori/sortering)
  - `GET api/models/{id}`
  - `GET api/categories`
- `network/RetrofitClient.kt` — bygger Retrofit-instansen med Moshi-converter och en OkHttp-interceptor som lägger till `X-API-Key`-headern på varje request automatiskt

### 6. Repository + ViewModel
- `repository/ModelRepository.kt` — enkelt lager som anropar API:t och hanterar fel (nätverksfel, 401, 404) som ett sealed result-typ, t.ex. `sealed class ApiResult<T> { data class Success<T>(val data: T): ApiResult<T>(); data class Error(val message: String): ApiResult<Nothing>() }`
- `viewmodel/ModelListViewModel.kt` och `viewmodel/ModelDetailViewModel.kt` — hämtar data via repository, exponerar state via `StateFlow`

### 7. Navigation
- Sätt upp `navigation-compose` i `MainActivity.kt` med en `NavHost`
- Två routes: `"modelList"` och `"modelDetail/{modelId}"`

### 8. Listvy-skärm
`ui/ModelListScreen.kt`:
- Visar modeller i en `LazyColumn`
- Varje rad: bild (om URL finns, annars platshållare), namn, status, kr/del-värde med enkel färgindikator (kan vara en simpel `Box` med bakgrundsfärg för nu — full färgtröskel-logik kan förfinas i senare fas)
- Tryck på rad → navigerar till `modelDetail/{id}`
- Visa laddningsindikator och enkel felvy vid nätverksfel

### 9. Detaljvy-skärm
`ui/ModelDetailScreen.kt`:
- Visar all information för en modell: namn, delantal, kategori, status, lista över källor/priser med kr/del per källa
- Read-only i denna fas — ingen redigering, inga knappar för att ändra status eller lägga till källor än

## Verifiering
1. Bekräfta att Flask-servern körs på datorn och är nåbar på nätverket (samma test som i Fas 1: `curl` från en annan enhet mot `http://DIN_SERVER_IP:5000/api/models` med API-nyckeln)
2. Bygg och kör appen på emulator ELLER din fysiska telefon — **telefonen måste vara på samma WiFi som datorn** om ni testar på fysisk enhet
3. Bekräfta att listvyn faktiskt visar riktiga modeller (inte tom lista, inte krasch)
4. Tryck på en modell, bekräfta att detaljvyn visar rätt data för just den modellen
5. Stäng av WiFi på servern eller stoppa Flask tillfälligt, bekräfta att appen visar ett rimligt felmeddelande istället för att krascha
6. Kontrollera Logcat i Android Studio för eventuella varningar/fel som inte syns i UI:t

## Avslutning
- Skapa `CLAUDE.md` och `TODO.md` i `C:\BrickRadarApp` (finns troligen inte än — skapa dem om de saknas) med:
  - Projektöversikt: native Android-app mot BrickRadar Flask-API
  - Var `ApiConfig.kt` ligger och hur IP/nyckel uppdateras om servern flyttar
  - Lista implementerade skärmar och vad som INTE är implementerat än (redigering, statusändring, kategorisering, MOC-hantering, VPN-stöd)
  - Notera att server-adress/API-nyckel är hårdkodade med avsikt just nu (beslut taget 2026-07-27), inte en bugg
