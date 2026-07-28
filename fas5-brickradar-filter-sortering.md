# Kickoff: BrickRadar Android – Fas 5: Filtrering, kategori och sortering i listvyn

## Bakgrund
Detaljvyn har nu full read/write-funktionalitet (status, redigering, källhantering, allt verifierat i Fas 3–4). Listvyn (`ModelListScreen.kt`) visar dock bara alla modeller okategoriserat och osorterat. Denna fas lägger till samma filter-/sorteringsmöjligheter som webb-UI:t har.

API:t stödjer redan detta (från Fas 1): `GET /api/models` accepterar query-parametrar för status, kategori, kr/del-sortering och "senast ändrad"-sortering. Verifiera exakta parameternamn och tillåtna värden genom att läsa `mould-king-tracker/api.py` (funktionen som hanterar `GET /api/models`) innan ni kodar — gissa inte parameternamnen.

## Uppgifter

### 1. Filter-UI ovanför listan
I `ModelListScreen.kt`, lägg till en filterrad högst upp (kan vara horisontellt scrollbar chip-rad):
- **Status-filter**: chips för Alla / Sök / Bevakar / Äger / Avslagen (en vald åt gången, "Alla" = ingen statusfilter skickas)
- **Kategori-filter**: dropdown eller chip-rad baserad på `GET /api/categories`-svaret (F1/racerbilar, sportbilar, grävmaskiner, lastbilar, motorcyklar, flygplan/helikoptrar, övrigt) — inkludera "Alla kategorier" som default

### 2. Sortering
Lägg till en sorteringsväljare (t.ex. ikon-knapp som öppnar en meny, eller dropdown) med alternativen:
- Kr/del (lägst till högst)
- Senast ändrad (nyast först)
- Namn (A-Ö)
- Standard/ingen specifik sortering (serverns default-ordning)

### 3. Koppla ihop filter/sortering med API-anrop
- Uppdatera `viewmodel/ModelListViewModel.kt` så att ändrad status, kategori eller sortering triggar ett nytt anrop till `GET /api/models` med rätt query-parametrar
- Undvik onödiga dubbel-anrop: om användaren snabbt växlar flera filter i rad, avbryt (cancel) föregående pågående nätverksanrop innan nästa skickas (t.ex. med en `Job`-referens i ViewModel som cancelas)
- Visa en laddningsindikator under tiden nya resultat hämtas, men behåll gärna föregående lista synlig (inte blank skärm) tills nya resultatet kommer, för mindre "flimmer"

### 4. Tomt resultat
Om filterkombinationen ger noll träffar: visa ett tydligt meddelande ("Inga modeller matchar valda filter") istället för en tom, förvirrande lista

### 5. Bevara filterval vid navigation
Om användaren går in i en modells detaljvy och sen tillbaka till listan, ska filtren/sorteringen som var valda fortfarande vara aktiva (dvs. spara filterstate i ViewModel, som redan överlever navigation inom samma NavHost-scope om det är korrekt uppsatt — verifiera att detta faktiskt stämmer i praktiken)

## Verifiering
1. Testa varje statusfilter (Sök/Bevakar/Äger/Avslagen/Alla) och bekräfta att listan uppdateras korrekt för varje
2. Testa kategorifilter, bekräfta korrekt resultat
3. Testa kombination av status + kategori samtidigt
4. Testa alla sorteringsalternativ, bekräfta att ordningen faktiskt stämmer (särskilt kr/del — kolla att lägsta värdet verkligen hamnar överst)
5. Välj en filterkombination som du vet ger noll träffar, bekräfta att det tomma-resultat-meddelandet visas korrekt
6. Gå in i en modell, tillbaka till listan, bekräfta att filtret fortfarande är aktivt
7. Snabbt växla mellan flera statusfilter i rad, bekräfta att listan inte "hackar" eller visar felaktiga mellanresultat (tecken på att gamla anrop inte avbröts korrekt)

## Avslutning
- Uppdatera `CLAUDE.md`/`TODO.md` i `C:\BrickRadarApp`:
  - Markera filtrering/sortering som klar
  - Kvarstående: lägg till ny modell manuellt (nästa fas), Brick4-sökning (fas därefter)
