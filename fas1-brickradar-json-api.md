# Kickoff: BrickRadar – Fas 1: JSON REST-API för Android-app

## Bakgrund
Vi bygger en native Android-app (Kotlin/Compose) som ska prata med samma Flask-backend som webb-UI:t redan använder (`C:\mould-king-tracker`). Innan Android-arbetet kan börja behöver backend exponera all funktionalitet som JSON-endpoints under `/api/`, parallellt med de befintliga HTML-routes (som INTE ska ändras eller tas bort).

Appen ska ha full funktionsparitet med webben, så API:t måste täcka: listvy, detaljvy, statusflöde, lägga till/redigera modell, källhantering/priser, kategorisering.

Detta kommer köras enbart på hemma-WiFi i första läget (ingen VPN ännu), men lägg ändå in ett enkelt API-nyckel-skydd så det är förberett för när VPN/Unraid är på plats senare.

## Uppgifter

### 1. Kartlägg befintliga routes först
Innan ni skriver någon kod: läs igenom `app.py` och identifiera alla nuvarande routes (sök efter `@app.route`). Lista dem och vilken data/logik varje route använder, så att API-endpoints kan återanvända samma underliggande funktioner (t.ex. `classify_value()`, `_enrich_price_with_shipping_and_value`) istället för att duplicera logik.

### 2. Skapa API-blueprint
- Skapa en ny fil, t.ex. `api.py`, med en Flask Blueprint registrerad under prefix `/api`
- Registrera blueprinten i `app.py`

### 3. Enkel API-nyckel-autentisering
- Lägg till en `API_KEY` i en config/`.env`-fil (ny fil, lägg till i `.gitignore` om den inte redan finns där)
- En `before_request`-decorator på API-blueprinten som kollar headern `X-API-Key` mot `API_KEY`, returnerar 401 om den saknas/är fel
- Undanta INGA endpoints – appen ska alltid skicka nyckeln

### 4. Endpoints att implementera
Bygg endpoints som speglar varje sak man kan göra i webb-UI:t idag:

- `GET /api/models` – lista alla modeller, med stöd för samma filter/sortering som main-listan idag (status, kategori, kr/del-sortering, "senast ändrad"-sortering)
- `GET /api/models/<id>` – detaljvy: all data för en modell inkl. källor/priser, kr/del, klassificering (färgtier)
- `POST /api/models` – skapa ny modell (manuell eller via modellnummer-sökning mot Brick4, samma logik som "+ Ny modell"-formuläret)
- `PUT /api/models/<id>` – redigera modell (namn, delantal, kategori, status)
- `PATCH /api/models/<id>/status` – ändra status (Sök/Bevakar/Äger/Avslagen)
- `POST /api/models/<id>/sources` – lägg till en källa/butik-rad manuellt (URL, lagerstatus, lagerland, valuta, leveranstid)
- `PUT /api/sources/<id>` – redigera en källrad
- `DELETE /api/sources/<id>` – ta bort en källrad
- `GET /api/categories` – lista kategorier + "bästa kr/del per kategori"-data
- `POST /api/models/<id>/refresh` – trigga manuell prisuppdatering för en modell (samma som "uppdatera priser"-knappen)

Varje endpoint ska returnera JSON med tydliga fältnamn (engelska nycklar är okej även om UI-text är svensk), och rimliga HTTP-statuskoder (200/201/400/404/401).

### 5. Felhantering & loggning
- Alla API-fel ska returneras som JSON: `{"error": "beskrivning"}` med rätt statuskod, aldrig en HTML-felsida
- Återanvänd befintlig loggning (samma detaljnivå som redan finns för scraping-fel) så fel även syns i den vanliga loggen

### 6. CORS (om det behövs)
Om Android-appen anropar API:t direkt (inte via WebView) behövs troligen INTE CORS, men dubbelkolla att Flask inte blockerar anrop från annan enhet på nätverket. Bekräfta att `app.run(host="0.0.0.0", ...)` fortfarande är satt (bekräftat i tidigare session).

## Verifiering
1. Starta Flask-servern, kontrollera att befintlig webb-UI fortfarande fungerar exakt som innan (inga regressions)
2. Testa varje endpoint med `curl` eller Postman från datorn själv:
   - `curl -H "X-API-Key: <nyckel>" http://localhost:5000/api/models`
   - Verifiera 401 utan nyckel
3. Testa minst `GET /api/models` och `GET /api/models/<id>` från en annan enhet på samma WiFi (t.ex. telefonens webbläsare eller `curl` om möjligt) mot datorns lokala IP, t.ex. `http://192.168.X.X:5000/api/models`, för att bekräfta att nätverksåtkomsten fungerar innan Android-appen byggs
4. Kontrollera att alla svar är giltig JSON (inte HTML-felsidor) även vid fel input (t.ex. `GET /api/models/99999`)

## Avslutning
- Uppdatera `CLAUDE.md` och `TODO.md`:
  - Ny sektion "JSON API (Fas 1 – Android)" med lista över implementerade endpoints och var API-nyckeln konfigureras
  - Notera att detta är förberett för framtida VPN-åtkomst (Fas med Unraid, ej påbörjad än)
  - Flagga som TODO: lägg till CORS/säkerhetsgranskning innan API:t exponeras utanför hemmanätverket
