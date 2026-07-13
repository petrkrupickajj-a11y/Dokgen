# Generátor dokumentů ze šablon

Webová appka v Javě (Spring Boot): naklikáš údaje klienta do formuláře,
uloží se do databáze, a z libovolné Word šablony pak jedním kliknutím
vygeneruješ hotový .docx dokument s doplněnými údaji.

Žádné AI, žádné externí API — čistě Java + databáze + práce se soubory.

## Stažení a první spuštění

Potřebuješ jen **Java 17+** (JDK, ne jen JRE) nainstalovanou na počítači —
nic dalšího (Maven, databázi...) instalovat nemusíš, projekt si vše potřebné
stáhne/obsahuje sám. Ověříš to příkazem `java -version` v terminálu.

**Stažení projektu** — jedna ze dvou možností:

- **Přes git** (pokud ho máš nainstalovaný):
  ```bash
  git clone https://github.com/petrkrupickajj-a11y/Dokgen.git
  cd Dokgen
  ```
- **Bez gitu** — na GitHubu klikni na zelené tlačítko **Code → Download ZIP**,
  stáhnutý archiv rozbal a v terminálu se přepni do té složky.

> Složka projektu musí ležet na cestě **bez diakritiky a mezer** (např.
> `C:\dev\dokgen`, ne na Ploše s českým názvem složky) - jinak na Windows
> padá spring-boot-maven-plugin na chybě `ClassNotFoundException` kvůli
> tomu, jak si interně sestavuje classpath.

**Spuštění:**

```bash
./mvnw spring-boot:run        # Linux/Mac
.\mvnw.cmd spring-boot:run     # Windows
```

První spuštění chvíli trvá (Maven si stáhne závislosti), appka pak naběhne
na **http://localhost:8080** a v terminálu uvidíš řádek
`Started DokgenApplication`. Appka je celá za přihlášením - hned po otevření
tě přesměruje na `/login`. Uživatelské jméno je **admin**, heslo appka při
prvním startu buď vezme z proměnné prostředí `DOKGEN_HESLO`, nebo (pokud
proměnná není nastavená) náhodně vygeneruje a **vypíše přímo do terminálu**
- hledej v logu řádek "vygenerovala toto NÁHODNÉ jednorázové heslo" (víc
v sekci Bezpečnost níže). Chceš-li si heslo nastavit sám předem:

```bash
DOKGEN_HESLO=tajneheslo123 ./mvnw spring-boot:run     # Linux/Mac
$env:DOKGEN_HESLO='tajneheslo123'; .\mvnw.cmd spring-boot:run   # Windows PowerShell
```

Appku vypneš v terminálu klávesou `Ctrl+C`.

## Co to používá

| Vrstva | Technologie |
|---|---|
| Web / formuláře | Spring Boot + Thymeleaf |
| Přihlašování | Spring Security (formulářový login, role Admin/Asistentka) |
| Databáze | H2 (soubor na disku, není potřeba nic instalovat) |
| Ukládání dat | Spring Data JPA / Hibernate |
| Práce s Word soubory | Apache POI |
| Export do PDF | Apache PDFBox |

## Co všechno appka umí (přehled stránek)

- `/` — seznam klientů, hledatelný podle jména, telefonu nebo emailu
  (parametr `?hledat=`)
- `/novy` — přidat klienta
- `/generovat/{id}` — vybrat šablonu a formát (Word/PDF) a stáhnout vyplněný
  dokument, nebo si ho tlačítkem "Náhled" (`/generovat/{id}/nahled`) nejdřív
  otevřít v novém okně jako PDF bez stažení a bez zápisu do historie
- `/sablony` (jen role `ADMIN`, viz Bezpečnost) — správa šablon (upload,
  stažení, nahrazení obsahu, smazání - i vestavěné, a to natrvalo; appka si
  smazání vestavěné šablony pamatuje, takže ji restart neobnoví). Každé
  nahrazení obsahu si appka pamatuje jako novou verzi (`/sablony/{id}/verze`)
  - odsud jde starší verzi kdykoli stáhnout nebo obnovit jako aktuální obsah
  (obnovení navíc uloží i tu právě přepisovanou verzi, takže se nikdy nic
  nenávratně neztratí)
- `/historie` — historie vygenerovaných dokumentů (filtrovatelná podle měsíce a jména klienta).
  Appka si vedle audit záznamu ukládá i skutečný vygenerovaný soubor
  (`./data/generated-documents/{id}.docx` nebo `.pdf`), takže u záznamů, ke
  kterým appka soubor pořád má, jde tlačítkem "Zobrazit" otevřít PDF rovnou v
  prohlížeči, nebo Word dokument ve výchozí aplikaci na tomhle počítači
  (stejný princip jako "Upravit" u šablon, viz níže). Starší záznamy z doby
  před zavedením téhle funkce soubor nemají - tlačítko se u nich nezobrazí.
  Appka fyzické soubory starší než `dokgen.vygenerovane-dokumenty.uchovat-dny`
  (výchozí 90 dní) sama maže při každém startu a pak jednou denně
  (`GenerovaneDokumentyUklidRunner`) - audit záznam v historii tím zůstane
  zachovaný navždy, jen tlačítko "Zobrazit" u něj přestane fungovat, stejně
  jako u starších záznamů popsaných výše.

Databáze se ukládá do souboru `./data/dokgen.mv.db` — data ti tedy
zůstanou zachovaná i po restartu.

Appka umí **česky, anglicky a německy** — přepínač (CS/EN/DE) je v pravém
horním rohu na každé stránce (v navigaci, nebo jako plovoucí odkazy na
stránkách bez navigace jako přihlášení). Zvolený jazyk appka pamatuje v
cookie, takže přežije i zavření prohlížeče. Překládá se opravdu všechno,
co appka sama posílá do prohlížeče - popisky, tlačítka, validační hlášky
formuláře i chybové stránky (viz `messages*.properties` a `LocaleConfig`) -
jediné, co zůstává vždy česky, je obsah samotných generovaných dokumentů
(vestavěné šablony smluv/faktur apod. - to je obsah dokumentu, ne text
appky).

Appka má teplý, "papírový" vzhled (krémová/terakotová paleta, Fraunces na
nadpisy, Inter na zbytek) - celé CSS je sdílené v jednom souboru
(`src/main/resources/static/styles.css`), takže se paleta i typografie dá
změnit na jednom místě. Na hlavních stránkách je vpravo dole i kontextová
nápověda (plovoucí tlačítko s krátkým vysvětlením dané stránky) a u pár
méně zjevných funkcí drobné ikonky s tooltipem.

## Jak to funguje uvnitř

1. **`Klient`** / **`Sablona`** / **`SablonaVerze`** / **`VygenerovanyDokument`**
   (entity) — klient, metadata šablony a jejích starších verzí (skutečné
   `.docx` soubory leží mimo databázi na disku, viz `SablonaUlozisteService`)
   a záznam o každém vygenerovaném dokumentu pro `/historie`
2. **`KlientRepository`** a spol. — Spring Data JPA nám samo vytvoří CRUD operace
3. **`DocumentGeneratorService`** — načte .docx šablonu, projde všechny odstavce
   (i v tabulkách) a nahradí placeholdery typu `${jmeno}` skutečnými hodnotami
   z databáze; stejná třída se stará i o verzování šablon (uložení/obnovení
   starší verze, viz `/sablony/{id}/verze`)
4. **`PdfExportService`** — když uživatel zvolí formát PDF, převede už vyplněný
   .docx do PDF přes Apache PDFBox (jen prosté vykreslení textu, nezachovává
   formátování Wordu 1:1)
5. **`KlientController`** / **`SablonaController`** / **`HistorieController`** —
   webové endpointy, které tohle všechno propojí a pošlou hotový dokument
   uživateli ke stažení
6. **`SecurityConfig`** / **`DokgenUserDetailsService`** / **`Role`** /
   **`PrihlaseniOmezovac`** — přihlášení, role (`ADMIN`/`ASISTENTKA`) a ochrana
   proti opakovanému zkoušení hesel (viz sekce Bezpečnost níže)
7. **`NavigaceModelAdvice`** / **`GlobalExceptionHandler`** — první schová
   odkazy na stránky, ke kterým přihlášená role stejně nemá přístup, druhý
   zajistí, že appka nikdy neukáže Spring "Whitelabel Error Page", ale
   vždy srozumitelnou `chyba.html`
8. **`GenerovaneDokumentyUklidRunner`** — při startu appky a pak jednou denně
   smaže z disku fyzické soubory starých vygenerovaných dokumentů (viz
   sekce `/historie` výše)

## Jak přidat vlastní šablonu

1. Vytvoř ve Wordu nový dokument (smlouva, nabídka, cokoliv) a na místa,
   kam mají přijít údaje, napiš přesně tyto placeholdery:

   ```
   ${jmeno}  ${prijmeni}  ${telefon}  ${email}
   ${adresa}  ${mesto}  ${psc}  ${ico}  ${poznamka}  ${datum}
   ```

2. Ulož ho jako `.docx` a nahraj přímo v appce na stránce `/sablony`
   (sekce "Nahrát novou šablonu"). Appka si soubor uloží na disk
   (`./data/word-templates/`) a metadata do databáze (entita `Sablona`) -
   žádnou úpravu kódu ani restart appky to nevyžaduje, nová šablona se
   hned objeví v nabídce na `/generovat/{id}`.

Existující šablonu (i vestavěnou) jde na `/sablony` kdykoli stáhnout, upravit
mimo appku (ve Wordu, Google dokumentech...) a nahrát zpět tlačítkem
"Nahradit", nebo smazat úplně.

Tlačítko **"Upravit"** otevře soubor šablony rovnou ve výchozí aplikaci pro
.docx na tomhle počítači (`java.awt.Desktop.open(...)`, stejné, jako by na
soubor uživatel dvakrat klikl v Průzkumníku) - appka pak automaticky vidí
uloženou změnu, protože jde o ten samý soubor na disku. **Funguje to jen při
lokálním běhu appky na stroji s grafickým rozhraním** - na headless serveru
(typicky vzdálené nasazení) appka žádnou výchozí aplikaci nemá jak spustit
a místo pádu appka zobrazí srozumitelnou chybovou stránku.

Pět vestavěných šablon (smlouva, cenová nabídka, faktura, protokol o předání,
plná moc) jde od základu přegenerovat i programově přes `SablonyGenerator`
(Apache POI) - výstup skončí v `src/main/resources/word-templates/`, odkud
si ho appka při prvním startu (přes `SablonySeeder`) sama načte do databáze:

```bash
./mvnw compile
java -cp target/classes cz.petrk.dokgen.tools.SablonyGenerator
```

## Přidání dalšího pole (např. DIČ, číslo účtu...)

Potřeba upravit na třech místech:

1. `Klient.java` — přidat pole + getter/setter
2. `formular.html` — přidat `<input th:field="*{novePole}">`
3. `DocumentGeneratorService.sestavData()` — přidat `data.put("${novePole}", ...)`

## Poznámka k Apache POI

Word interně rozseká viditelný text do několika XML "runů" (kvůli historii
úprav, kontrole pravopisu apod.), takže hledání `${jmeno}` na úrovni jednoho
runu často nefunguje. `DocumentGeneratorService.nahradVOdstavci()` proto bere
text celého odstavce najednou, provede nahrazení, a pak ho vloží zpátky
do jediného runu. Kvůli tomu odstavec může přijít o vnitřní formátování
(např. jen část tučně) — pro šablony s placeholdery to ale v drtivé většině
případů nevadí.

## Bezpečnost a validace

- Formulář klienta má serverovou validaci (`jakarta.validation` anotace na
  `Klient`) - povinná jména, formát emailu/telefonu (české číslo)/PSČ/IČO.
- Chyby (neexistující klient/šablona, chybějící/poškozená šablona, neočekávané
  výjimky) se zobrazí jako srozumitelná stránka `chyba.html`, ne jako Spring
  "Whitelabel Error Page".
- H2 konzole (`/h2-console`) je v defaultu **vypnutá** - obsahuje osobní údaje
  klientů. Pro lokální ladění spusť s `-Dspring-boot.run.profiles=dev`.
- **Appka je celá za přihlášením** (Spring Security) - obsahuje osobní údaje
  klientů, takže nic v ní není přístupné bez přihlášení, včetně `/registrace`
  (viz níže). Uživatelé se ukládají v databázi (entita `Uzivatel`, heslo jako
  BCrypt hash).
  - **Role** (`Role` - `ADMIN` / `ASISTENTKA`) omezují, co účet smí (viz
    `SecurityConfig`, `DokgenUserDetailsService`). `ADMIN` smí vše včetně
    správy šablon (`/sablony/**`) a přidávání dalších účtů (`/registrace`).
    `ASISTENTKA` smí jen spravovat klienty a generovat dokumenty - na šablony
    ani na správu účtů se nedostane (nav odkaz "Šablony" se jí ani nezobrazí,
    a přímý pokus o URL appka odmítne srozumitelnou stránkou "Přístup
    odepřen" místo pádu appky).
  - **Výchozí účty** se při prvním startu nahrají z `application.properties`
    (`dokgen.uzivatele`): `admin` (role `ADMIN`) a `asistentka` (role
    `ASISTENTKA`). Heslo se bere z proměnných prostředí `DOKGEN_HESLO` /
    `DOKGEN_HESLO_ASISTENTKA` - pokud nejsou nastavené, appka při prvním
    startu každému z nich **vygeneruje náhodné jednorázové heslo a vypíše ho
    do logu** (stejný princip jako vestavěné Spring Security hlášení "Using
    generated security password"). Appka tedy nikdy neběží se
    známým/uhodnutelným výchozím heslem. Po prvním startu appka bere účty
    výhradně z databáze - úprava properties později už nic nepřepíše.
  - **Přidání dalšího účtu** (např. pro nového kolegu) jde přes `/registrace`
    (jméno, heslo 2×/min. 6 znaků, a role) - ale jen pro už přihlášeného
    `ADMIN`a, ne veřejná samoobslužná registrace. Odkaz na tuto stránku není
    nikde v navigaci appky - kdo ji potřebuje použít, musí znát přímo URL
    `/registrace`.
  - **Zapomenuté heslo** se dá napravit přes konzoli (appka nemusí ani běžet
    na webu, jen se spustí s navíc argumentem, změní/vytvoří heslo v databázi
    a hned skončí):
    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--zmenit-heslo=jmeno:nove-heslo"
    ```
    Reset hesla přes e-mailový odkaz appka záměrně nemá - vyžadovalo by to
    SMTP server/účet, což je přesně ta externí závislost, které se tenhle
    projekt drží stranou (viz "Žádné AI, žádné externí API" v úvodu).
  - **Ochrana proti zkoušení hesel** - `PrihlaseniOmezovac` si v paměti drží
    počet neúspěšných pokusů na každé uživatelské jméno; po 5 neúspěších za
    sebou je jméno na 15 minut zamčené (Spring Security ho odmítne ještě
    před kontrolou hesla). Funguje stejně pro neexistující jména, takže appka
    nikde neprozradí, který účet v ní existuje.
- CSRF ochrana je zapnutá (Spring Security default) - všechny formuláře v
  appce ji používají automaticky přes Thymeleaf.
- Upload šablony (.docx = ZIP archiv) je chráněný proti "zip bombě" -
  `PoiBezpecnostConfig` nastavuje limit poměru komprese a maximální velikost
  jednoho souboru v archivu, podezřelý soubor appka rovnou odmítne.

## Testy

```bash
./mvnw test
```

Pokrývá generování dokumentů, správu šablon včetně ochrany proti zip bombě a
verzování (`DocumentGeneratorServiceTest`), export do PDF včetně lámání
dlouhých slov (`PdfExportServiceTest`), historii a stránkování
(`HistorieServiceTest`), založení účtu včetně volby role
(`RegistraceServiceTest`), načítání uživatelů a promítnutí role i zámku účtu
do autorit (`DokgenUserDetailsServiceTest`), generování hesel při prvním
startu a doplnění role u starších účtů (`UzivateleSeederTest`), ochranu proti
zkoušení hesel (`PrihlaseniOmezovacTest`), napojení na přihlašovací události
Spring Security (`PrihlaseniUdalostiListenerTest`), seedování vestavěných
šablon (`SablonySeederTest`), úklid starých vygenerovaných dokumentů
(`GenerovaneDokumentyUklidRunnerTest`), sanitizaci názvů stahovaných souborů
(`NazevSouboruTest`), chybové stránky místo Spring "Whitelabel Error Page"
(`GlobalExceptionHandlerTest`), schování nedostupných odkazů v navigaci podle
role (`NavigaceModelAdviceTest`) a webové endpointy včetně validace a
chybových stavů napříč všemi controllery - klienti a generování dokumentů
(`KlientControllerTest`), správa šablon (`SablonaControllerTest`), přidání
účtu (`RegistraceControllerTest`), filtr v historii (`HistorieControllerTest`),
přihlašovací stránka (`PrihlaseniControllerTest`) a skutečné vynucení rolí na
úrovni `SecurityConfig` (`RoliAOpravneniTest`).
