# Kickoff: BrickRadar Android – Fas 3: Statusändring + redigera modell

## Bakgrund
Grundskelettet (Fas 2) är klart och verifierat: listvy och detaljvy visar riktig data från Flask-API:t utan krascher. Detaljvyn är dock helt read-only just nu. Den här fasen lägger till de två första skrivande funktionerna: ändra status och redigera grundläggande modellfält.

API-endpoints finns redan (från Fas 1, verifierade):
- `PATCH /api/models/<id>/status` — ändra status
- `PUT /api/models/<id>` — redigera modell (namn, delantal, kategori, status)

## Uppgifter

### 1. Statusändring i detaljvyn
I `ModelDetailScreen.kt`:
- Lägg till en tydlig UI-komponent för att visa och ändra status — t.ex. en rad med fyra knappar/chips (Sök, Bevakar, Äger, Avslagen) där aktuell status är visuellt markerad (t.ex. fylld bakgrund) och tryck på en annan status triggar ändringen
- Vid tryck: anropa `PATCH /api/models/{id}/status` via ett nytt repository-anrop, visa en laddningsindikator på just den komponenten under anropet
- Vid lyckat svar: uppdatera lokal state direkt (optimistisk uppdatering är okej, men bekräfta med svaret från servern)
- Vid fel: visa ett felmeddelande (t.ex. Snackbar) och återställ till föregående status i UI:t

### 2. Redigera modell — grundfält
Lägg till en redigeringsläge för detaljvyn:
- En redigera-knapp (pennikon) i toppen av `ModelDetailScreen.kt` som togglar mellan visningsläge och redigeringsläge
- I redigeringsläge: textfält för namn, numeriskt fält för delantal, dropdown/chips för kategori (använd samma kategorilista som `GET /api/categories` returnerar)
- En "Spara"-knapp som anropar `PUT /api/models/{id}` med de ändrade fälten, och en "Avbryt"-knapp som återgår utan att spara
- Validering: delantal måste vara ett positivt heltal, namn får inte vara tomt — visa inline-felmeddelande om valideringen misslyckas, blockera spara-anrop tills det är korrekt

### 3. Repository- och ViewModel-uppdateringar
- Utöka `network/BrickRadarApi.kt` med metoderna för `PATCH .../status` och `PUT /models/{id}` om de inte redan finns som interface-metoder
- Utöka `repository/ModelRepository.kt` med motsvarande funktioner, samma `ApiResult`-mönster som redan används för läsning
- Utöka `viewmodel/ModelDetailViewModel.kt` med funktioner `updateStatus(newStatus: String)` och `updateModel(updatedFields: ...)`, som anropar repository och uppdaterar `StateFlow` med resultatet

### 4. UI-feedback
- All spara-/statusändring ska ge tydlig visuell feedback: laddningsspinner under anropet, en kort bekräftelse (Snackbar eller liknande) vid lyckad sparning, tydligt felmeddelande vid fel
- Detaljvyn ska INTE navigera bort automatiskt efter en lyckad statusändring — användaren stannar kvar och ser den uppdaterade statusen direkt

## Verifiering
1. Öppna en modell i appen, ändra status från t.ex. "Bevakar" till "Äger" — bekräfta att UI:t uppdateras direkt
2. Gå tillbaka till listvyn och tillbaka till samma modell igen — bekräfta att statusen kvarstår (dvs. det sparades verkligen på servern, inte bara lokalt i minnet)
3. Öppna webb-UI:t i en vanlig webbläsare samtidigt och bekräfta att statusändringen syns där också (samma databas)
4. Testa att redigera namn och delantal på en modell, spara, och verifiera på samma sätt (i appen efter omnavigering, och i webb-UI:t)
5. Testa valideringen: försök spara ett tomt namn eller negativt delantal, bekräfta att det blockeras med ett tydligt felmeddelande
6. Testa felhantering: stäng av Flask-servern tillfälligt, försök ändra status, bekräfta att appen visar ett rimligt felmeddelande istället för att krascha

## Avslutning
- Uppdatera `CLAUDE.md`/`TODO.md` i `C:\BrickRadarApp`:
  - Markera statusändring och grundläggande redigering som klara
  - Notera vad som INTE är implementerat än: lägga till/redigera/ta bort källor, lägga till ny modell, kategorisering/filtrering i listvyn (kommande faser)
