# Kickoff: BrickRadar Android – Fas 6: Lägg till ny modell manuellt

## Bakgrund
Appen har nu full funktionalitet för befintliga modeller (visa, filtrera, redigera, statusändra, hantera källor). Den här fasen lägger till möjligheten att skapa en helt ny modell direkt i appen — men **enbart manuell inmatning i detta steg**, ingen Brick4-sökning än (det blir en separat fas därefter).

API-endpoint finns redan: `POST /api/models`. Läs igenom exakt vilka fält den förväntar sig i `mould-king-tracker/api.py` innan ni bygger formuläret — särskilt vilka fält som är obligatoriska kontra valfria, och hur den skiljer på "manuellt tillagd modell" kontra en modell som normalt hade kommit från Brick4-sökning (det finns ett känt öppet problem sedan tidigare: manuellt tillagda modeller på webben har bara tillåtit namnredigering, inte delantal — kontrollera om detta är fixat i backend eller om appen behöver ett workaround/fält som ändå skickas med rätt värde från start).

## Uppgifter

### 1. Ny skärm: "Lägg till modell"
Skapa `ui/AddModelScreen.kt` med ett formulär:
- Namn (textfält, obligatoriskt)
- Modellnummer (textfält, valfritt för manuellt tillagda — notera i UI:t att detta kan lämnas tomt för MOC/anpassade set)
- Delantal (numeriskt fält, obligatoriskt — krävs för att kr/del ska kunna beräknas senare)
- Kategori (dropdown från `GET /api/categories`, med möjlighet att välja "Övrigt"/ingen kategori)
- Status vid skapande (dropdown: Sök/Bevakar/Äger/Avslagen, default "Sök")
- Bildlänk (textfält, valfritt — URL till en bild, om ni vill stödja det redan nu; annars hoppa över och notera som framtida förbättring)

### 2. Navigation till nya skärmen
- Lägg till en synlig "+"-flytande actionknapp (FAB) i `ModelListScreen.kt`
- Ny route i `NavHost`: `"addModel"`

### 3. Validering
- Namn och delantal obligatoriska, delantal måste vara positivt heltal
- Visa tydliga inline-felmeddelanden, blockera spara-anrop tills korrekt

### 4. Repository + ViewModel
- Lägg till `POST models` i `network/BrickRadarApi.kt`
- Lägg till motsvarande funktion i `repository/ModelRepository.kt`
- Skapa `viewmodel/AddModelViewModel.kt` med state för formulärfälten och en `saveModel()`-funktion

### 5. Efter lyckad skapelse
- Navigera tillbaka till listvyn
- Visa en kort bekräftelse (Snackbar: "Modellen har lagts till")
- Den nya modellen ska synas i listan direkt (antingen genom att listvyn hämtar om data vid återkomst, eller genom att lägga till den lokalt i state — omhämtning från servern är säkrast för konsekvens)

### 6. Felhantering
- Om servern avvisar (t.ex. valideringsfel som inte fångades i appens egen validering), visa serverns felmeddelande i ett Snackbar utan att stänga formuläret eller tappa ifyllda värden

## Verifiering
1. Lägg till en helt ny modell med alla fält ifyllda, bekräfta att den syns korrekt i listvyn med rätt namn/delantal/kategori/status
2. Öppna modellen i detaljvyn, bekräfta att alla fält stämmer och att kr/del kan beräknas när en källa läggs till (dvs. delantal sparades korrekt — detta är det kända problemet från webben som ska vara löst här)
3. Öppna webb-UI:t, bekräfta att samma modell syns där också
4. Testa att lägga till en modell utan modellnummer (simulerar ett MOC/anpassat set), bekräfta att det går igenom utan fel
5. Testa validering: försök spara utan namn, eller med bokstäver i delantal-fältet, bekräfta att det blockeras med tydligt felmeddelande
6. Testa avbryt-flödet: fyll i formuläret delvis, navigera bort utan att spara, bekräfta att ingen ofullständig modell skapades av misstag

## Avslutning
- Uppdatera `CLAUDE.md`/`TODO.md` i `C:\BrickRadarApp`:
  - Markera manuell modelltillägg som klar
  - Notera om det kända delantal-problemet från webben (manuellt tillagda modeller saknade delantal-redigering) bekräftades vara löst i backend eller om det krävde ett appspecifikt workaround
  - Kvarstående: Brick4-sökning vid modelltillägg (nästa fas)
