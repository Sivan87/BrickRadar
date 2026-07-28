# TODO

## Fas 2 — kvarstående verifiering (kräver fysisk enhet/emulator, inte gjort av Claude)
- [ ] Bygg och kör appen på emulator eller fysisk telefon (telefonen måste vara på samma WiFi som datorn, `192.168.1.30`)
- [ ] Bekräfta att listvyn visar riktiga modeller (inte tom lista, inte krasch)
- [ ] Tryck på en modell, bekräfta att detaljvyn visar rätt data
- [ ] Stäng av Flask/WiFi tillfälligt, bekräfta rimligt felmeddelande istället för krasch
- [ ] Kolla Logcat för varningar/fel som inte syns i UI:t

## Fas 3 — klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Statusändring i detaljvyn (chips Sök/Bevakar/Äger/Avslagen, `PATCH /api/models/{id}/status`)
- [x] Redigera modell — namn, delantal, kategori (pennikon → redigeringsläge, `PUT /api/models/{id}`)
- [x] Validering (tomt namn, icke-positivt delantal) och felhantering (Snackbar) klientsidan
- [ ] Bygg och kör på enhet: ändra status, gå tillbaka/fram, bekräfta att det sparades på servern (inte bara lokalt)
- [ ] Bekräfta samma sak i webb-UI:t samtidigt (samma databas)
- [ ] Testa redigera namn/delantal, samma verifiering
- [ ] Testa validering (tomt namn, negativt delantal) och felhantering (stäng av Flask-servern tillfälligt)

## Fas 4 — klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Lägg till/redigera/ta bort källor i detaljvyn (`POST/PUT/DELETE /api/sources`, plus `POST/DELETE .../source-override` för leveranstid — se CLAUDE.md om varför det är ett separat anrop)
- [x] Validering (tom URL, icke-positivt pris) och felhantering (Snackbar, formuläret stängs inte vid fel) klientsidan
- [ ] Bygg och kör på enhet: lägg till en källa på en testmodell, bekräfta att den syns direkt i appens lista
- [ ] Öppna webb-UI:t samtidigt, uppdatera sidan, bekräfta att samma källa syns där
- [ ] Redigera priset på en källa i appen, spara, bekräfta i webb-UI:t att det nya priset visas — och att det INTE skapat en dubblettrad (append-only-beteendet, se CLAUDE.md)
- [ ] Ta bort en källa i appen, bekräfta bekräftelsedialogen (avbryt en gång, ta bort en gång), att den försvinner i både appen och webb-UI:t
- [ ] Testa validering: lägg till en källa utan URL eller med negativt pris, bekräfta att det blockeras
- [ ] Sätt en leveranstid på en källa i appen, bekräfta att den syns i webb-UI:ts "Lagerstatus/lagerland/leveranstid"-dialog (separat lagringsplats från själva prisraden, se CLAUDE.md)
- [ ] Bekräfta att kr/del-beräkningen uppdateras korrekt efter att en källa lagts till/ändrats/tagits bort

## Fas 5 — klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Statusfilter (Alla/Sök/Bevakar/Äger/Avslagen) och kategorifilter (från `GET /api/categories`) som chip-rader högst upp i listvyn
- [x] Sorteringsmeny: kr/del (lägst-högst), senast ändrad (nyast), namn (A-Ö, sorteras klientsidan — API:t stöder det inte), standard
- [x] Filter/sortering triggar nytt `GET /api/models`-anrop med rätt query-parametrar (`status`/`category`/`sort`), pågående anrop avbryts vid snabb filterväxling
- [x] Föregående lista syns kvar (med laddningsindikator) vid nytt filter istället för blank skärm; tydligt meddelande vid noll träffar
- [ ] Bygg och kör på enhet: testa varje statusfilter, kategorifilter, kombinationer, alla sorteringsalternativ (särskilt att kr/del-sorteringen faktiskt stämmer)
- [ ] Bekräfta noll-träffar-meddelandet med en filterkombination som garanterat ger tomt resultat
- [ ] Gå in i en modell och tillbaka, bekräfta att filter/sortering fortfarande är aktiva
- [ ] Snabbt växla mellan flera statusfilter i rad, bekräfta att listan inte "hackar" eller visar felaktiga mellanresultat

## Fas 6 — klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Ny skärm `AddModelScreen` (Namn/Modellnummer/Märke/Delantal/Kategori/Status/Bildlänk), FAB i listvyn, route `addModel`
- [x] Validering (tomt namn, tomt märke, icke-positivt delantal) blockerar Spara, felmeddelanden klientsidan
- [x] `POST /api/models` kopplat, plus uppföljande `PUT /api/models/{id}` om vald kategori skiljer sig från auto-gissningen (se CLAUDE.md om varför `category` ignoreras av `POST /models`)
- [x] Lyckad skapelse: navigerar tillbaka, listan hämtas om, Snackbar-bekräftelse (via `NavBackStackEntry.savedStateHandle`, se CLAUDE.md)
- [x] Serverfel (dubblett/valideringsfel): Snackbar med serverns felmeddelande, formuläret stängs inte, värden tappas inte
- [ ] Bygg och kör på enhet: lägg till en helt ny modell med alla fält ifyllda, bekräfta att den syns korrekt i listvyn (namn/delantal/kategori/status)
- [ ] Öppna modellen i detaljvyn, bekräfta att alla fält stämmer och att kr/del kan beräknas när en källa läggs till (delantal sparades korrekt)
- [ ] Öppna webb-UI:t, bekräfta att samma modell syns där också
- [ ] Testa att lägga till en modell utan modellnummer (märke "Generic", simulerar ett MOC/anpassat set), bekräfta att det går igenom utan fel
- [ ] Testa validering: försök spara utan namn, utan märke, eller med bokstäver i delantal-fältet, bekräfta att det blockeras
- [ ] Testa avbryt-flödet: fyll i formuläret delvis, navigera bort (pil tillbaka) utan att spara, bekräfta att ingen ofullständig modell skapades
- [ ] Testa att välja en explicit kategori (annan än "Gissa från namn"), bekräfta att den faktiska kategorin i listan/detaljvyn matchar valet (inte auto-gissningen) — verifierar den uppföljande PUT-anropet
- [ ] Testa att lägga till en modell med modellnummer+märke som redan finns, bekräfta att servern avvisar med 409 och rätt felmeddelande visas

## Fas 7 — klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Läge-val i `AddModelScreen` ("Sök modellnummer", default / "Fyll i manuellt", Fas 6:s formulär oförändrat)
- [x] Sökfält + `GET /api/brick4/search-by-number` (fanns redan i backend, ingen ändring i `mould-king-tracker` behövdes — se CLAUDE.md)
- [x] 0/1/flera-träffar-hantering (felmeddelande+"fyll i manuellt istället" / auto-vald kandidat / kandidatlista att välja mellan)
- [x] Bekräftelsesteg efter val: modellnummer+märke låst från Brick4-svaret, namn/delantal VALFRIA (fylls i av `initial_fetch` i bakgrunden om tomma, se CLAUDE.md om varför — söksvaret innehåller inte namn/bild/delantal)
- [x] `POST /api/models` (`name`/`piece_count` nullable, ny `CategoryUpdateRequest` för den uppföljande kategori-PUT:en så den inte skickar med tomma/gissade värden och riskerar skriva över det bakgrundshämtningen redan satt)
- [ ] Bygg och kör på enhet: sök på ett modellnummer med ett känt enda-träff-märke (t.ex. en vanlig CaDA-modell), bekräfta att märket auto-väljs och att modellen skapas korrekt
- [ ] Sök på ett modellnummer som ger flera märkeskandidater, bekräfta att alla visas och att rätt en kan väljas
- [ ] Sök på ett påhittat/obefintligt modellnummer, bekräfta felmeddelande + att "fyll i manuellt istället" växlar läge med numret ifyllt
- [ ] Skapa en modell via sökning UTAN att fylla i namn/delantal manuellt, öppna den i detaljvyn efter några sekunder, bekräfta att bakgrundshämtningen fyllt i namn/bild/delantal korrekt
- [ ] Jämför med webb-UI:t: sök samma modellnummer där (samma `/api/brick4/search-by-number`), bekräfta konsekventa resultat
- [ ] Testa "Ändra märke" i bekräftelsesteget, bekräfta att det går tillbaka till kandidatlistan utan att tappa sökresultaten
- [ ] Testa ett känt tidigare problematiskt modellnummer (bokstavssuffix, t.ex. Mould King 13108-familjen) om möjligt — enligt `mould-king-tracker/CLAUDE.md` (2026-07-20) redan löst server-side (`_brick4_number_candidates`), men inte verifierat från Android-sidan

## Fas 9 — auto-update, klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Backend: `version.json`, `static/downloads/`, `GET /api/app-version`, `publish-update.py` (verifierat live denna session mot den redan körande lokala servern — se `mould-king-tracker/CLAUDE.md`, "App-uppdatering")
- [x] Android: versionskoll vid appstart (`UpdateViewModel`, jämför mot `BuildConfig.VERSION_CODE`), uppdateringsdialog, `DownloadManager`-nedladdning, `FileProvider`+installations-intent, "installera okända appar"-behörighetsflöde
- [ ] Bygg och kör på enhet: sänk tillfälligt den installerade appens `versionCode` (eller höj server-`version.json`s), starta appen, bekräfta att uppdateringsdialogen dyker upp med rätt `versionName`/`releaseNotes`
- [ ] Tryck "Uppdatera" med "installera okända appar" REDAN beviljad sedan tidigare, bekräfta direkt nedladdning + installationsprompt utan extra dialog
- [ ] Tryck "Uppdatera" med behörigheten INTE beviljad, bekräfta förklaringsdialogen → `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` → efter beviljande fortsätter flödet automatiskt till nedladdning
- [ ] Stäng av WiFi/servern innan appstart, bekräfta att appen startar helt normalt utan felruta/krasch (fail-silent-kravet)
- [ ] Installera den "nya" versionen via flödet, starta om appen, bekräfta att uppdateringsdialogen INTE dyker upp igen (versionCode matchar nu)
- [ ] Bekräfta att telefonen faktiskt kräver ett godkännande-tryck för installationen (Android-säkerhetskrav, ingen tyst installation) och att appdata bevaras efteråt (samma keystore, se "Release build" i CLAUDE.md)

## Fas 10 — Statistik-flik, klart (kod), kräver fysisk enhet/emulator för verifiering (inte gjort av Claude)
- [x] Bottennavens "Priser"-flik omdöpt till "Statistik", `enabled = true`, ny route `statistik` i `MainActivity`
- [x] Ny `StatistikScreen`/`StatistikViewModel`, återanvänder befintligt `GET /api/stats` (`avgKrPerPieceCloneAll`/`avgKrPerPieceLegoAll`), inga backend- eller DTO-ändringar behövdes
- [x] `assembleDebug` går igenom
- [ ] Bygg och kör på enhet: tryck "Statistik" i bottennaven, bekräfta att skärmen öppnas och stängs (tillbaka-pilen) korrekt
- [ ] Bekräfta att de visade kr/del-snitten (Klon/LEGO) matchar samma siffror som webb-UI:ts statistikvy för samma data
- [ ] Stäng av Flask-servern tillfälligt, öppna Statistik-fliken, bekräfta felvyn + att "Försök igen" fungerar när servern kommer tillbaka
- [ ] Bekräfta att "Mer"-fliken fortfarande är nedtonad/inaktiv och inte råkade aktiveras av misstag

## Nästa faser
- [ ] MOC-hantering utöver "Generic"-märket (t.ex. egen bilduppladdning)
- [ ] VPN/Unraid-åtkomst + inställningsskärm för server-IP/nyckel (ersätter `BuildConfig`-värdena i `ApiConfig.kt` — flyttades dit från hårdkodade konstanter 2026-07-28, se issue #2 och CLAUDE.md "Serverkonfiguration", men kräver fortfarande ett ombygge för varje IP/nyckel-byte tills detta görs)
- [ ] Manuell prisuppdatering (`POST .../refresh`) från detaljvyn
