# Kickoff: BrickRadar Android – Fas 7: Brick4-sökning vid modelltillägg

## Bakgrund
"Lägg till modell"-flödet har hittills bara stött manuell inmatning (Fas 6). Denna fas lägger till möjligheten att söka efter ett modellnummer mot Brick4 (samma katalogkälla som webb-UI:t använder), få tillbaka matchande resultat (namn, bild, delantal, varumärke), och skapa modellen baserat på ett valt sökresultat istället för att fylla i allt manuellt.

**Läs igenom detta noggrant innan ni kodar**, eftersom Brick4-sökningen på backend-sidan har flera kända särdrag och begränsningar från tidigare arbete:
- Brand-matchning kan misslyckas för vissa varumärken som saknas i `BRICK4_BRAND_IDS` (känt exempel: YC-GC004/悦创/盘古 saknades tidigare — kontrollera om det är löst)
- Vissa modellnummervarianter (t.ex. suffix som "S") kan misslyckas trots att basmodellen fungerar (känt exempel: Mould King 13108S)
- HiTian-liknande URL-slug-problem gäller INTE Brick4 vad vi vet, men verifiera ändå att sökresultatens data stämmer mot den faktiska sidans titel/innehåll, inte bara URL:en

Undersök exakt vilken endpoint eller intern funktion Flask-backend redan använder för Brick4-sökning (troligen finns den redan som en del av webbens "+ Ny modell"-formulär i `mould-king-tracker/app.py` eller `api.py`) — **återanvänd den logiken via ett API-endpoint, bygg inte om sökningen från grunden i appen.**

## Uppgifter

### 1. Kontrollera/skapa sök-endpoint i backend
Om det inte redan finns ett rent API-endpoint för Brick4-sökning (separat från formulär-inskickning), lägg till ett i `mould-king-tracker/api.py`:
- `GET /api/brick4-search?q=<modellnummer>` — anropar samma interna sökfunktion som webben använder, returnerar JSON-lista med kandidater: namn, bild-URL, delantal, varumärke, Brick4-länk
- Om flera varumärken matchar samma nummer (tvetydigt resultat), returnera alla kandidater så appen kan visa ett urval, precis som webben gör vid tvetydighet

**Detta steg kan kräva en egen liten Claude Code-runda i `mould-king-tracker` INNAN Android-delen påbörjas** — om sökfunktionen inte redan är exponerad som ett rent JSON-endpoint, gör det arbetet i mould-king-tracker-projektet först, verifiera med curl, och återkom sedan till Android-delen av denna kickoff.

### 2. Sökfält i "Lägg till modell"-flödet
I `ui/AddModelScreen.kt` (byggd i Fas 6), lägg till ett läge-val högst upp:
- **"Sök modellnummer"** (nytt, default) vs **"Fyll i manuellt"** (befintligt flöde från Fas 6)
- I sökläge: ett textfält för modellnummer + en sökknapp

### 3. Visa sökresultat
- Vid sökning: anropa `GET /api/brick4-search`, visa resultat som en lista med kort (bild, namn, varumärke, delantal) i en `LazyColumn` eller liknande
- Om noll resultat: visa tydligt meddelande, erbjud "Fyll i manuellt istället"-knapp som växlar till manuellt läge (Fas 6-formuläret) med modellnumret redan ifyllt
- Om ett resultat: visa det direkt med en "Använd denna"-knapp
- Om flera resultat (tvetydigt varumärke): visa alla, låt användaren välja rätt

### 4. Skapa modell från valt sökresultat
- Vid val: förifyll ett bekräftelseformulär (kan återanvända delar av Fas 6:s formulär) med data från Brick4 — namn, delantal, varumärke/kategori-förslag, bild
- Låt användaren justera kategori/status innan de bekräftar (samma fält som i manuellt-flödet), sedan `POST /api/models` med den kompletta datan

### 5. Bildhantering
- Om sökresultatet innehåller en bild-URL: visa den i förhandsvisningen och skicka med den vid skapande (om `POST /api/models` stödjer ett bildfält — kontrollera detta i backend)
- Om bilden inte kan laddas (trasig länk): visa en platshållarikon, låt användaren ändå gå vidare utan bild

### 6. Repository + ViewModel
- Lägg till `GET brick4-search` i `network/BrickRadarApi.kt`
- Utöka `repository/ModelRepository.kt` och `viewmodel/AddModelViewModel.kt` med sökstate (laddning, resultat, fel) separat från det manuella formulärets state

## Verifiering
1. Sök på ett modellnummer som ni vet ger ett tydligt enda resultat (t.ex. en vanlig CaDA- eller Mould King-modell), bekräfta att rätt data visas och att modellen skapas korrekt vid bekräftelse
2. Sök på ett nummer som ger flera varumärkes-kandidater, bekräfta att alla visas och att rätt en kan väljas
3. Sök på ett nummer som inte ger några träffar, bekräfta felmeddelande + "fyll i manuellt"-vägen fungerar med numret förifyllt
4. Testa ett känt problematiskt modellnummer om möjligt (t.ex. en variant med bokstavssuffix) och dokumentera resultatet — fungerar det, eller kvarstår samma begränsning som i webben? Notera i `TODO.md` om det senare
5. Skapa en modell via sökning, öppna den i detaljvyn efteråt, bekräfta att delantal/namn/bild stämmer mot vad Brick4 faktiskt visade
6. Jämför med webb-UI:t: sök samma modellnummer där, bekräfta att resultaten är konsekventa mellan webb och app

## Avslutning
- Uppdatera `CLAUDE.md`/`TODO.md` i **båda** projekten (`mould-king-tracker` om backend-ändringar gjordes där, och `BrickRadarApp`):
  - Markera Brick4-sökning i appen som klar
  - Dokumentera eventuella kvarstående kända begränsningar (brand-matchning, suffix-varianter) som fortfarande gäller
  - Med detta är alla grundläggande funktioner (#1–#5 från ursprungsplanen) klara — nästa steg blir UI/design-polish, som ni beslutat vänta med tills nu
