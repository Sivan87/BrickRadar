# Kickoff: BrickRadar Android – Fas 4: Hantera källor (lägg till/redigera/ta bort)

## Bakgrund
Fas 3 (statusändring + redigera modell) är klar och verifierad. Nästa steg: låta användaren hantera källor/butik-rader direkt i detaljvyn — samma funktionalitet som webb-UI:t redan har, men i appen.

API-endpoints finns redan (från Fas 1, verifierade):
- `POST /api/models/<id>/sources` — lägg till en källa
- `PUT /api/sources/<id>` — redigera en källa
- `DELETE /api/sources/<id>` — ta bort en källa

**OBS — viktig detalj att verifiera innan ni kodar:** i Fas 2 nämnde Claude Code att käll-redigering i backend är "append-only" (append-only source-edit behavior). Läs igenom hur `PUT /api/sources/<id>` faktiskt beter sig i `mould-king-tracker/api.py` innan ni bygger UI:t — om servern skapar en ny rad/historikpost vid varje "redigering" snarare än att skriva över, måste appens UI och lokala state hantera detta korrekt (t.ex. visa senaste versionen, inte dubbletter).

## Uppgifter

### 1. Datamodell för källa — komplettera fält
Utöka `Source`-datamodellen (om fält saknas) med samtliga skrivbara fält: URL, pris, valuta, lagerstatus (i lager/ej i lager), lagerland, leveranstid. Kontrollera exakt fältnamn och typer mot vad `GET /api/models/{id}` faktiskt returnerar för `sources`-listan (inspektera ett live-svar om osäker).

### 2. Fasta listor för dropdowns
Skapa `app/src/main/java/com/sivan/brickradar/model/Constants.kt` (eller liknande) med:
```kotlin
val CURRENCIES = listOf("SEK", "USD", "EUR", "CNY")

data class CountryOption(val code: String, val displayName: String, val flagEmoji: String)

val COUNTRIES = listOf(
    CountryOption("CN", "Kina", "🇨🇳"),
    CountryOption("SE", "Sverige", "🇸🇪"),
    CountryOption("US", "USA", "🇺🇸"),
    CountryOption("EU", "EU", "🇪🇺"),
    CountryOption("GB", "Storbritannien", "🇬🇧"),
    CountryOption("DE", "Tyskland", "🇩🇪")
)
```
Verifiera mot webb-UI:t (`mould-king-tracker/static/app.js` eller `templates/`) om samma lista med länder/valutor redan används där — återanvänd samma värden för konsekvens mellan webb och app, lägg till fler länder i listan om webben har fler.

### 3. UI: lista över källor i detaljvyn
I `ModelDetailScreen.kt`, för varje källa i listan:
- Visa alla fält kompakt (butiksnamn/URL, pris + valuta, lagerstatus-ikon, landsflagga, leveranstid)
- En redigera-ikon (penna) och en ta bort-ikon (papperskorg) per rad
- Tryck på redigera → öppnar samma formulär som "lägg till ny källa" men förifyllt med befintliga värden

### 4. UI: lägg till ny källa
- En tydlig "+ Lägg till källa"-knapp längst ner i källistan
- Öppnar ett formulär (kan vara en `AlertDialog`, `ModalBottomSheet`, eller separat skärm — bottom sheet rekommenderas för mobil UX) med fält:
  - URL (textfält)
  - Pris (numeriskt fält)
  - Valuta (dropdown från `CURRENCIES`)
  - Lagerstatus (switch/checkbox: i lager / ej i lager)
  - Lagerland (dropdown från `COUNTRIES`, visa flagga + namn)
  - Leveranstid (textfält, fritext är okej t.ex. "2-3 veckor")
- Validering: URL och pris obligatoriska, pris måste vara positivt tal

### 5. Ta bort källa
- Tryck på papperskorg-ikonen → visa en bekräftelsedialog ("Ta bort källan från [butiksnamn]?") innan `DELETE /api/sources/{id}` anropas — undvik oavsiktlig borttagning
- Vid lyckad borttagning: ta bort raden från listan direkt i UI:t

### 6. Repository + ViewModel
- Utöka `network/BrickRadarApi.kt` med `POST models/{id}/sources`, `PUT sources/{id}`, `DELETE sources/{id}`
- Utöka `repository/ModelRepository.kt` med motsvarande funktioner
- Utöka `viewmodel/ModelDetailViewModel.kt` med `addSource(...)`, `updateSource(...)`, `deleteSource(sourceId)` — alla ska uppdatera lokal state baserat på serverns svar (hämta om hela modellen efter en ändring är den säkraste vägen om append-only-beteendet gör lokal state-hantering knepig, se punkt om detta ovan)

### 7. Felhantering
- Nätverksfel eller valideringsfel från servern (t.ex. ogiltig URL) ska visas tydligt (Snackbar) utan att stänga formuläret, så användaren kan rätta och försöka igen

## Verifiering
1. Lägg till en ny källa på en testmodell i appen — bekräfta att den syns direkt i appens lista
2. Öppna webb-UI:t samtidigt, uppdatera sidan, bekräfta att samma källa syns där
3. Redigera priset på en källa i appen, spara, bekräfta i webb-UI:t att det nya priset visas (och — viktigt — att det INTE skapat en dubblettrad om backend är append-only)
4. Ta bort en källa i appen, bekräfta bekräftelsedialogen fungerar (avbryt en gång, ta bort en gång), och att den försvinner både i appen och i webb-UI:t
5. Testa validering: försök lägga till en källa utan URL eller med negativt pris, bekräfta att det blockeras
6. Bekräfta att kr/del-beräkningen (som redan visas från Fas 2) uppdateras korrekt efter att en källa lagts till/ändrats/tagits bort — ladda om modellen och kolla att värdet stämmer

## Avslutning
- Uppdatera `CLAUDE.md`/`TODO.md` i `C:\BrickRadarApp`:
  - Markera källhantering (lägg till/redigera/ta bort) som klar
  - Dokumentera om backend visade sig vara append-only för källredigering, och hur appen hanterar det
  - Kvarstående: lägg till ny modell (sök mot Brick4/manuell), kategorisering/filtrering i listvyn
