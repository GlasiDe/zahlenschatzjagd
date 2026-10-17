# LEHRPLAN.md

Verbindliche, destillierte Fachvorgabe für die Aufgaben-Generierung. Diese Datei ist
bindend (vgl. `CLAUDE.md`, Abschnitt „Projekt-Dokumentation"). Sie hält die
didaktischen Regeln fest, an denen sich `createTask` (`GameLogic.kt`) orientiert, damit
die App inhaltlich zum Lehrplan passt und Erweiterungen konsistent bleiben.

## Quelle

Lehrplan Mathematik für die Primarstufe in Nordrhein-Westfalen (Stand 18.03.2025):
<https://lehrplannavigator.nrw.de/system/files/media/document/file/ps_lp_m_einzeldatei_2025_03_18.pdf>

Das PDF wird **bewusst nicht** ins Repo eingecheckt (urheberrechtlich geschütztes
Dokument des Landes NRW, außerdem als Volltext keine brauchbare Arbeitsvorlage). Statt
dessen werden hier nur die für die App relevanten Regeln destilliert. Bei Zweifeln gilt
das verlinkte Original.

Jedes wörtliche Zitat in dieser Datei ist im verlinkten Original auffindbar — wer eines
ergänzt, schlägt es vorher dort nach, statt es sinngemäß wiederzugeben.

## Zahlenräume nach Klassenstufe (laut Lehrplan)

| Klasse | Zahlenraum   |
|--------|--------------|
| 1      | bis 20       |
| 2      | bis 100      |
| 3      | bis 1.000    |
| 4      | bis 1.000.000|

Kleines Einmaleins: Grundaufgaben **1×1 bis 10×10** sollen beherrscht werden. Division
wird als Umkehroperation der Multiplikation verstanden.

**Vorsicht bei dieser Tabelle:** Sie ist eine Destillat-Hilfe, keine Gliederung des Lehrplans.
Der Lehrplan selbst kennt nur **zwei** Zeitpunkte für Kompetenzerwartungen — Ende der
**Schuleingangsphase** und Ende Klasse 4 (siehe `CONTEXT.md`). Die Schuleingangsphase umfasst
die Klassen 1 und 2 und dauert ein bis drei Jahre
(<https://www.schulministerium.nrw/lernen-der-grundschule>). Was dort unter „Ende der
Schuleingangsphase" steht, gilt also bis Ende Klasse 2 — der Zahlenraum bis 100 gehört dazu.

## In der App aktuell umgesetzte Zahlenräume

Auswählbar in `SelectionScreen.kt`: **10 / 20 / 50 / 100** (jeweils „0 bis N"). Damit
deckt die App im Wesentlichen Klasse 1 (bis 20) und Klasse 2 (bis 100) ab. Die
Zwischenstufen 10 und 50 sind didaktische Feinabstufungen, kein eigener Lehrplan-Wert.

Übungsformen mit eigenem Auswahl-Bildschirm bieten davon nur eine **Teilmenge** an — neue
Werte kommen dort keine dazu. Nachbarzahlen lässt in `NachbarzahlenSelectionScreen.kt` die
Wahl zwischen 20 und 100; Zahlenmauer, Verdoppeln und Einmaleins geben den Zahlenraum ganz vor.

## Regeln, die `createTask` einhält (Ist-Stand)

- **Plus:** beide Summanden 0..range, Summe bleibt ≤ range (keine Überschreitung des
  Zahlenraums).
- **Minus:** kein negatives Ergebnis (`num2 ≤ num1`).
- **Mal:** beide Faktoren aus dem kleinen Einmaleins, zusätzlich auf `sqrt(range)`
  gedeckelt (max. 10). Dadurch bleibt das Produkt automatisch ≤ range. Konsequenz:
  bei kleinem Zahlenraum bewusst kleine Faktoren (z. B. range 10 → Faktoren 0..3).
- **Geteilt:** glatte Division (Ergebnis ganzzahlig); **Divisor ist einstellig** und
  nie 0; Divisor und Quotient teilen sich dieselbe `sqrt(range)`-Deckelung, sodass der
  Dividend im Zahlenraum bleibt.
- **Verliebte Zahlen:** Zerlegung einer festen Zielzahl (aktuell **10**) in zwei Summanden.
  Aufgabenform `num1 + _ = Zielzahl` – gesucht ist der „verliebte Partner" (`Zielzahl −
  num1`). `num1` liegt in `0..Zielzahl`. Didaktisch gehört das zu Klasse 1 (Zahlzerlegung /
  Zahlenraum bis 10). **Abweichende Trivial-Regel:** Nur der Fall `num1 == 0` bzw.
  `num1 == Zielzahl` (also `0 + 10` / `10 + 0`) wird reduziert. Der Fall mit 1 (`1 + 9`,
  `9 + 1`) wird **bewusst nicht** unterdrückt – die Partnerzerlegungen mit 1 sind hier keine
  trivialen Aufgaben, sondern erwünschter Lernstoff. Zwei Schwierigkeitsstufen mit denselben
  Aufgaben: „Mit Hilfe" zeigt ein Münz-Zehnerfeld, „Ohne Hilfe" nicht
  (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
- **Größer/Kleiner (Zahlvergleich):** Zwei Zahlen `a, b ∈ 0..range` werden verglichen; gesucht
  ist das passende Relationszeichen zwischen ihnen. *Lehrplan-Bezug:* Das **Ordnen und
  Vergleichen** von Zahlen (nach „größer als" / „kleiner als") ist eine Kompetenzerwartung im
  Bereich „Zahlen und Operationen", Schwerpunkt „Zahlverständnis"; die Relationszeichen `<`,
  `>`, `=` gehören zur zugelassenen fachbezogenen **Symbolik der Grundschule** (siehe Quelle
  oben – der 2025er-Lehrplan bzw. die NRW-Richtlinien Mathematik Grundschule). `a` und `b`
  werden unabhängig aus `0..range` gezogen, `a == b` ist dabei **ausdrücklich zugelassen** –
  alle drei Relationszeichen `<`, `=`, `>` kommen vor. **Keine 0/1-Reduktion:** `0 < 5`,
  `1 > 0` usw. sind hier legitime, sinnvolle Vergleiche; es gibt keine Ausschlussregel (anders
  als z. B. bei „Verliebte Zahlen"). Zahlenraum wählbar wie bei den Rechenarten (10/20/50/100).
- **Triviale Aufgaben reduziert:** Operanden/Ergebnisse mit 0 oder 1 werden per
  Rejection-Sampling stark seltener erzeugt (≈ 90 % Unterdrückung bei 0, ≈ 70 % bei 1),
  aber nicht vollständig verboten. Vier Ausnahmen, jeweils dort begründet: „Verliebte
  Zahlen" reduziert nur 0/Zielzahl, „Größer/Kleiner" und „Nachbarzahlen" reduzieren gar
  nicht — dort ist eine Aufgabe mit 0 oder 1 keine triviale —, und das „Einmaleins" kennt
  keine 0 und zieht `1 · n` so oft wie jede andere Aufgabe der Reihe.
- **Verdoppeln:** Ausgangszahl `num1 ∈ 0..10` (10 als Sonderfall bewusst eingeschlossen,
  obwohl bereits zweistellig), gesucht ist das Doppelte (`correctAnswer = num1 * 2`,
  Ergebnis 0..20). *Lehrplan-Bezug:* „Verdoppeln und halbieren" wird explizit als
  Kopfrechenstrategie genannt (Kompetenzerwartungen Ende Schuleingangsphase, Bereich
  „Schnelles Kopfrechnen": „lösen Aufgaben zum schnellen Kopfrechnen im Zahlenraum bis
  100 (… verdoppeln und halbieren)"). Der Zahlenraum 0..10 für die Ausgangszahl ist
  zusätzlich als **Kernaufgaben-Basis** belegt: „geben die Kernaufgaben automatisiert
  wieder und leiten weitere Aufgaben des kleinen Einmaleins daraus ab" (ebenfalls Ende
  Schuleingangsphase); für Klasse 4 wird das zu „geben alle Zahlensätze des kleinen
  Einmaleins automatisiert wieder" gesteigert. Die Automatisierung der
  Verdopplungs-Kernaufgaben ist damit über die gesamte Grundschulzeit relevant, nicht
  nur für Klasse 1 – die App-Einschränkung auf 0..10 betrifft nur den Zahlenraum der
  Übung, nicht die Zielgruppe. **Anzeige:** Die Aufgabe wird bewusst **ohne**
  Rechenzeichen gestellt (nicht als `num1 + num1 = _` oder `num1 · 2 = _`), sondern als
  reine Verdoppeln-Anweisung zur Ausgangszahl – die Äquivalenz zu beiden Rechenarten
  wird erst im Feedback nach der Antwort gezeigt, damit Verdoppeln als eigenständiges
  Konzept erkannt wird, das mit Addition und Multiplikation übereinstimmt. Die Malaufgabe
  steht dort als `2 · num1` („zweimal die Zahl"), nicht als `num1 · 2` – so lesen
  Grundschulkinder sie leichter (Rückmeldung einer Grundschul-Mathelehrkraft).
  **Distraktor-Regel (leicht-Modus mit Auswahl-Antworten):** Die zwei falschen Optionen
  werden aus zwei gemischten Fehler-Strategien gezogen – „off-by-one" am Ergebnis
  (`correctAnswer ± 1`) *und* Verdopplung der Nachbarzahl (`2 * (num1 ± 1)`) –, damit
  Kinder kein festes Auswahlmuster (z. B. „immer die mittlere Zahl") statt der
  Verdopplung selbst lernen können. Auch die Button-Position der richtigen Antwort wird
  zufällig gemischt.

## Zahlenmauer

Pyramide aus Zellen: eine Basis-Etage mit N Zahlen, jede Zelle einer höheren Etage ist die
Summe der zwei direkt darunterliegenden Nachbarzellen, bis zur Spitze (1 Zelle). *Lehrplan-
Bezug:* Zahlenmauern sind ein etabliertes Grundschul-Übungsformat zum Automatisieren von
Kopfrechnen und zum Erkennen von Zahlbeziehungen. Sie trainieren im Kern Addition und
ordnen sich damit dem Kompetenzbereich „Zahlen und Operationen" zu, Schwerpunkt
„Operationsverständnis": „nutzen und erklären die Zusammenhänge der Operationen
untereinander" (Kompetenzerwartungen Ende Klasse 4) sowie allgemeiner dem Anspruch,
„nicht-zählende Rechenstrategien" zu erwerben (Einleitung des Lehrplans).

**Vier Schwierigkeitsstufen** (fixe Kombination aus Etagenzahl, Zahlenraum, Aufgabenform –
bewusst **ein** Regler statt mehrerer, damit die Auswahl für Kinder überschaubar bleibt):

| Stufe  | Etagen | Zahlenraum | Aufgabenform                                  |
|--------|--------|------------|------------------------------------------------|
| Leicht | 3      | 20         | Aufbau (Basis vollständig vorgegeben)          |
| Mittel | 4      | 50         | Aufbau                                          |
| Schwer | 5      | 100        | Aufbau                                          |
| Ultra  | 5      | 100        | Umkehraufgabe (2 Basiszellen fehlen)           |

Alle vier Stufen nutzen ausschließlich die bereits etablierten Zahlenräume 10/20/50/100
(kein neuer Wert wie 1.000, siehe „Bei Erweiterung beachten" unten).

**Leicht/Mittel/Schwer – Aufbauaufgabe:** Die Basis ist immer vollständig vorgegeben; alle
übrigen Zellen werden durch Aufwärts-Addieren erschlossen. Bei `floors` Basiszellen ist die
Spitze `Σ C(floors-1, i) · x_i` (Pascalsche Gewichte, z. B. bei 5 Etagen `[1,4,6,4,1]`,
Summe 16). Damit die Spitze den Zahlenraum nie überschreitet, wird jede Basiszelle zunächst
auf eine sichere Obergrenze `safeCap = range / Σgewichte` begrenzt; danach wird ein zufällig
bestimmter **Teil** des verbleibenden Budgets (0 bis zum vollen Rest) randomisiert auf
einzelne Basiszellen verteilt (statt blindem Rejection-Sampling über den vollen Zahlenraum,
das bei mehr Etagen unzumutbar oft verwerfen müsste). Bewusst wird dabei **nicht** immer das
komplette verbleibende Budget verteilt, sonst würde die Spitze (die äußeren Basiszellen haben
stets Gewicht 1 und können so jeden Rest aufnehmen) praktisch immer exakt den Zahlenraum
erreichen statt über viele Mauern hinweg zu variieren. **Triviale Aufgaben (0/1) werden wie bei den übrigen Aufgabentypen per
Rejection-Sampling seltener erzeugt (≈ 90 % bei 0, ≈ 70 % bei 1).** Diese Reduktion wird nur
ausgesetzt, wenn `safeCap` sehr klein ist (&lt; 4), weil sie bei so wenigen möglichen Werten
die Verteilung zu stark auf den Maximalwert verzerren würde – bei den aktuellen
Zahlenräumen (20/50/100/100) liegt `safeCap` bei allen vier Stufen bei mindestens 5, die
Ausnahme greift also derzeit nie, bleibt aber als Absicherung für künftige, engere
Zahlenraum/Etagen-Kombinationen bestehen.

**Ultra – Umkehraufgabe als Rechenkontrolle:** Zwei nicht benachbarte Basiszellen fehlen;
dafür wird jeweils die direkt darüberliegende Elternzelle zusätzlich aufgedeckt, sodass die
fehlende Basiszahl durch einfache Rückwärts-Subtraktion (Elternzelle minus bekannte
Nachbarzelle) bestimmbar ist – nie ein Gleichungssystem mit mehreren Unbekannten. *Lehrplan-
Bezug:* Das Ableiten von Umkehraufgaben aus bekannten Aufgaben ist im Lehrplan wörtlich als
Kompetenzerwartung verankert, Bereich „Zahlen und Operationen", Schwerpunkt „Schnelles
Kopfrechnen": bereits am Ende der Schuleingangsphase „geben die Zahlensätze des kleinen
Einspluseins automatisiert wieder und leiten deren Umkehrungen sicher ab", am Ende der
Klasse 4 gesteigert zu „geben alle Zahlensätze des kleinen Einmaleins automatisiert wieder
und leiten deren Umkehraufgaben sicher ab". Ultra überträgt dieses Prinzip (Ergebnis der
Addition ist bekannt, ein Summand fehlt) auf die Zahlenmauer. Da hierbei zusätzlich zur
reinen Addition eine Rückwärts-Ableitung verlangt wird, ist Ultra spürbar anspruchsvoller als
Schwer trotz gleicher Etagenzahl und wird in der App entsprechend deutlich als
„sehr schwer" gekennzeichnet.

## Nachbarzahlen

Drei aufeinanderfolgende Zahlen stehen nebeneinander; **genau eine** ist vorgegeben, die beiden
anderen ergänzt das Kind. *Lehrplan-Bezug:* Bereich „Zahlen und Operationen", Schwerpunkt
**Zahlverständnis**, Kompetenzerwartungen **Ende der Schuleingangsphase**: „beschreiben
Beziehungen zwischen Zahlen und in Zahlenfolgen (u. a. ist der Vorgänger/Nachfolger von, ist die
Hälfte/das Doppelte von, ist um x kleiner/größer als)". In derselben Spalte — also ebenfalls zum
Ende der Schuleingangsphase — steht „ordnen und vergleichen Zahlen im Zahlenraum bis 100", und
die Schuleingangsphase reicht bis Ende Klasse 2 (siehe die Warnung zur Zahlenraum-Tabelle oben).
Damit deckt **eine** Kompetenzerwartung beide Zahlenräume der Übungsform. Das Wort Nachbarzahlen
selbst steht im Lehrplan an anderer Stelle, im Schwerpunkt **Zahlenrechnen** und ebenfalls zum
Ende der Schuleingangsphase: „rechnen vorteilhaft mithilfe von Zahlbeziehungen (u. a.
Nachbarzahlen) und Rechengesetzen (u. a. Kommutativgesetz)". Dort ist es eine Rechenstrategie,
hier die geübte Zahlbeziehung selbst.

**Zahlenraum 20 oder 100, frei wählbar** — und zwar **unabhängig von der Schwierigkeitsstufe**:
Beide Stufen gibt es in beiden Zahlenräumen, es sind also vier Kombinationen mit vier getrennten
Schatztruhen. Fachlich neu ist im großen Zahlenraum der **Zehnerübergang** (`39 | 40 | 41`) und
der obere Rand (`98 | 99 | 100`) — genau die Stellen, an denen die Zahlreihe nicht mehr durch
bloßes Weiterzählen der Einerstelle fortzuschreiben ist. Weitere Zahlenräume (10, 50, 1.000) gibt
es hier bewusst nicht.

**Schwierigkeitsstufe leicht** (`NACHBARZAHLEN_LEICHT`): Vorgegeben ist immer die **mittlere**
Zahl, gesucht sind Vorgänger und Nachfolger.

**Schwierigkeitsstufe schwer** (`NACHBARZAHLEN_SCHWER`): Je Aufgabe ist zufällig die **linke**,
die **mittlere** oder die **rechte** Zahl vorgegeben. Steht die vorgegebene Zahl außen, sind beide
gesuchten Zahlen auf derselben Seite — das Kind denkt dann durchgehend vorwärts („die 9 ist die
kleinste, welche kommen danach?") oder durchgehend rückwärts. Fachlich ist das dieselbe
Kompetenzerwartung, aber nicht mehr allein am Muster „eine Zahl links, eine Zahl rechts"
abzuarbeiten: Der Lehrplan verlangt, Beziehungen **in Zahlenfolgen** zu beschreiben, nicht nur den
unmittelbaren Vorgänger und Nachfolger zu nennen. Die Stufe sammelt **eigene** Abzeichen; ihr
Fortschritt zählt nicht für die leichte und umgekehrt — und beide zählen je Zahlenraum getrennt.

**Ränder.** Es gilt die eine Regel „alle drei Zahlen liegen im **gewählten** Zahlenraum". Daraus
folgt: die mittlere Zahl liegt in `1..N-1`, eine vorgegebene linke Zahl in `0..N-2`, eine
vorgegebene rechte in `2..N`.

| Zahlenraum | mittlere Zahl | vorgegebene linke Zahl | vorgegebene rechte Zahl | Reihen |
|---|---|---|---|---|
| 20  | `1..19` | `0..18` | `2..20`  | 19 |
| 100 | `1..99` | `0..98` | `2..100` | 99 |

**Gleichverteilt gezogen, ohne Zurücklegen.** Innerhalb einer Runde wiederholt sich keine Reihe,
solange der Vorrat reicht; ist er leer, beginnt er neu gemischt von vorn. In der schweren Stufe
zählt die Kombination aus Reihe **und** vorgegebener Position als Ziehung: Dieselbe Reihe einmal
von links und einmal von rechts ist für das Kind eine andere Aufgabe. Der Vorrat ist damit dreimal
so groß wie in der leichten Stufe.

**Eine Ausnahme von der reinen Gleichverteilung: die ersten drei Aufgaben der schweren Stufe.**
Der Vorrat stellt je eine Aufgabe pro Position an den Anfang, damit jede Runde alle drei Formen
zeigt. Ohne diese Schichtung fehlte in einer Runde von fünf Aufgaben (dem Minimum des Reglers) in
gut einem Drittel der Fälle eine Position — dann übte ein Kind eine ganze Runde lang nur das
gewohnte Muster. Welche Reihe je Position vorn steht und in welcher Reihenfolge die drei kommen,
bleibt Zufall; dahinter ist der Vorrat unverändert gemischt.

**Keine Bevorzugung von Zehnerübergängen.** Im Zahlenraum 20 gibt es nur 3 solche Reihen von 19
möglichen (16 %), im Zahlenraum 100 sind es 19 von 99 (19 %). Eine Quote von „jeder fünften
Aufgabe" wäre also wirkungslos und würde im kleinen Zahlenraum Wiederholungen erzwingen.

**Keine 0/1-Reduktion.** Anders als beim Rechnen ist `0 | 1 | 2` keine triviale Aufgabe, sondern
der Anfang der Zahlreihe — die Ausnahme zur Regel „Triviale Aufgaben reduziert" oben.

**Wertung.** Die ganze Reihe ist **eine** Aufgabe in der Historie und nur dann richtig, wenn
beide Zahlen stimmen (wie bei der Zahlenmauer). Einen zweiten Versuch gibt es nicht; korrigiert
wird beliebig oft **vor** dem Prüfen, weil ein Vertipper sonst die Aufgabe kostet und eine
automatische Auswertung bei ein-, zwei- und dreistelligen Zahlen nicht zuverlässig erkennen
kann, wann eine Eingabe fertig ist.

**Eingabe.** Eine Lücke nimmt höchstens so viele Ziffern an, wie der gewählte Zahlenraum braucht —
bis 20 zwei, bis 100 drei. So lässt sich die 100 eintippen, ohne dass eine vierstellige Eingabe
möglich wird. Gelöscht und neu getippt wird beliebig oft vor dem Prüfen.

**Anzeige.** Über den Lücken stehen die Pfeile ← und →, bewusst **keine** Buchstaben „V"/„N" aus
dem Schulheft: Die App richtet sich an Kinder, die noch nicht sicher lesen. Die Begründung steht
als Kommentar an `ui/nachbarzahlen/NachbarzahlenFeld.kt`. Steht die vorgegebene Zahl außen, zeigen
beide Pfeile in dieselbe Richtung.

**Vorlesen.** Gesprochen wird **einmal je Runde**, zur ersten Aufgabe — nicht zu jeder. Bei jeder
einzelnen war es am Gerät lästig, und keine andere Übungsform liest ihre
Aufgaben vor; Ella spricht dort nur beim Betreten eines Bildschirms. Ganz weglassen geht aber
nicht: Ein Kind, das noch nicht sicher liest, erführe sonst nicht, was hier zu tun ist.

Der Satz hängt an der vorgegebenen Stelle, nicht an der Stufe — so hört ein Kind,
das nicht liest, welche Zahl feststeht:

| vorgegeben | Ella sagt |
|---|---|
| Mitte | „Hier steht die 7. Welche Zahl kommt davor? Und welche kommt danach?" |
| links | „Hier steht die 9. Welche zwei Zahlen kommen danach?" |
| rechts | „Hier steht die 20. Welche zwei Zahlen kommen davor?" |

## Einmaleins

Gezieltes Üben einzelner **Reihen** des kleinen Einmaleins, der 2er- bis 10er-Reihe, in zwei
Schwierigkeitsstufen. *Lehrplan-Bezug:* Bereich „Zahlen und Operationen", Schwerpunkt **Schnelles
Kopfrechnen**. Zum Ende der Schuleingangsphase: „geben die Kernaufgaben automatisiert wieder und
leiten weitere Aufgaben des kleinen Einmaleins daraus ab"; zum Ende der Klasse 4 gesteigert zu
„geben alle Zahlensätze des kleinen Einmaleins automatisiert wieder und leiten deren
Umkehraufgaben sicher ab". Das automatisierte Wiedergeben ist genau das, was die Übungsform übt.
Die Umkehraufgaben (Geteilt) bleiben bewusst Sache der Grundrechenarten.

**Eine Reihe** sind die zehn Aufgaben `1 · n` bis `10 · n`; die Reihe steht immer **hinten**
(`8 · 7`, nie `7 · 8`). Die 1er-Reihe ist nicht wählbar – sie ist Zählen, nicht Einmaleins. Alle
Aufgaben bleiben im Zahlenraum bis 100.

**Aufsagen** (leichter): die Reihe der Reihe nach, zehn leere Kästchen ohne Aufgabe davor, in die
`7, 14, 21 …` kommen. Das ist zugleich eine Kompetenzerwartung aus dem Schwerpunkt
**Zahlverständnis** zum Ende der Schuleingangsphase: „zählen im Zahlenraum bis 100 (vorwärts,
rückwärts, in Schritten, beliebige Startzahl)", und unter Schnelles Kopfrechnen genauso: „zählen
vorwärts und rückwärts in Schritten". Ein falsches Kästchen wird sofort markiert und mit der
richtigen Zahl gefüllt, damit der Rest der Reihe darauf aufbauen kann. Eine Runde nimmt jede
gewählte Reihe einmal, aufsteigend.

**Durcheinander** (schwerer): einzelne Aufgaben `8 · 7 = _` aus allen gewählten Reihen, **ohne
Zurücklegen** gezogen. Erst wenn alle Aufgaben der gewählten Reihen einmal dran waren, wird neu
gemischt, und dieselbe Aufgabe kommt nie zweimal direkt hintereinander. So fragt eine Runde den
Zahlensatz ab, ohne dass die Reihenfolge der Reihe mithilft.

**Ausnahme von „Triviale Aufgaben reduziert": keine 0, keine Reduktion der 1.** `0 · n` kommt
nicht vor, weil die Reihe beim Faktor selbst beginnt. `1 · n` wird dagegen **nicht** unterdrückt,
anders als bei Mal in den Grundrechenarten: Beim Aufsagen ist sie der erste Schritt, ohne den
die Reihe nicht beginnt, und beim Durcheinander gehört sie zu „alle Zahlensätze" der Reihe. Eine
seltener gezogene `1 · n` hieße dort, dass Aufgaben der Reihe ungleich oft drankommen – gegen das
Ziehen ohne Zurücklegen.

## Bei Erweiterung beachten

Wer neue Aufgabentypen ergänzt oder den Zahlenraum vergrößert, richtet sich an diesen
Regeln aus:

- Neue/größere Zahlenräume an den Lehrplan-Stufen orientieren (nächste sinnvolle
  Erweiterung wäre Klasse 3, Zahlenraum bis 1.000).
- Multiplikation/Division: kleines Einmaleins als Obergrenze der Faktoren beibehalten,
  Division weiterhin nur mit einstelligem Divisor und glattem Ergebnis.
- Ergebnisse dürfen den gewählten Zahlenraum nicht überschreiten; keine negativen
  Ergebnisse in der Grundschul-Stufe.
- Die Reduktion trivialer 0/1-Aufgaben sinngemäß übernehmen, **wo eine Aufgabe mit 0 oder 1
  wirklich trivial ist**. Die bestehenden Ausnahmen stehen oben bei „Triviale Aufgaben
  reduziert"; wer sie für eine neue Übungsform aussetzt, schreibt den Grund dazu.
- Neue Regeln zuerst hier dokumentieren, dann in der Logik der Übungsform umsetzen
  (`GameLogic.kt` für die Rechenarten, sonst eine eigene Datei wie `ZahlenmauerLogic.kt` oder
  `NachbarzahlenLogic.kt`) und mit Unit-Tests absichern (Test-Pflicht, siehe `CLAUDE.md`).
