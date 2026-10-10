# Roadmap

Sammlung von Ideen für zukünftige Features. Dies ist eine lose Ideensammlung, keine
verbindliche Planung mit Reihenfolge oder Zeitplan.

Was tatsächlich umgesetzt werden soll, wandert von hier in ein
[GitHub-Issue](https://github.com/GlasiDe/zahlenschatzjagd/issues) und steht dann nicht mehr in
dieser Datei.

## Personalisierung
- **Gezieltes Üben statt Zufall**: Aufgaben, die ein Kind noch nicht sicher beherrscht,
  sollen häufiger drankommen als rein zufällig ausgewählte Aufgaben.

## Einfacherer Einstieg
- **Auswahl nach Klassenstufe**: Alternative, einfachere Auswahlmethode zur aktuellen
  manuellen Konfiguration (Rechenart/Zahlenraum/Anzahl) – Kind wählt nur "1., 2., 3.
  oder 4. Klasse" und die App stellt passende Aufgaben zusammen.
  *Vorbehalt:* Das ist bewusst noch eine grobe Idee und braucht ein hinterlegtes
  **Curriculum**, weil Rechenarten gestaffelt eingeführt werden (z. B. Division erst gegen
  Ende der 2. Klasse). Eine naive Auto-Auswahl würde sonst Aufgaben anbieten, die das Kind
  auf seiner Stufe noch gar nicht kann. Erfordert also mehrere didaktische Einzel-
  entscheidungen.

## Klasse 3 und 4
- **Größere Zahlenräume bis Ende der Grundschule**: Der Lehrplan reicht bis Klasse 4
  (Zahlenraum bis 1.000.000, siehe `LEHRPLAN.md`). Offen ist, ob größere Zahlenräume beim
  Kopfrechnen **je Rechenart** unterschiedlich weit gehen müssen – etwa Plus schon bis 1.000,
  Mal aber noch nicht. Dann bräuchte auch die Freigabe Zahlenräume je Übung statt eines
  gemeinsamen Satzes. *Vorher mit dem vollständigen Lehrplan abgleichen*, nicht aus der
  Zusammenfassung erschließen. Die heutige Freigabe verbaut den Weg nicht
  (`docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md`).

## Neue Übungsformen
- **Mathetricks**: Gezieltes Einüben von Rechentricks, z. B. Vertauschen der Zahlen
  bei Plusaufgaben (Kommutativgesetz) oder "Zwerg und Riese" (Aufgaben mit großem
  Abstand zwischen den Zahlen anders lösen als mit kleinem Abstand).
- **Zahlenreihen fortsetzen**: Aufgaben, bei denen eine begonnene Zahlenfolge
  logisch weitergeführt werden muss. *Nicht zu verwechseln mit der Übungsform
  „Nachbarzahlen":* Dort ist die Schrittweite immer 1 und steht fest, gesucht sind
  die Nachbarn einer vorgegebenen Zahl. Hier wäre gerade das Muster hinter der Folge (2er-,
  5er-, 10er-Schritte, rückwärts) das, was ein Kind erkennen muss.
- **Mathehaus**: Klassische Grundschul-Übungsform, bei der eine Zahl im "Dach" eines
  Hauses steht und die Kästchen darunter mit passenden Rechnungen befüllt werden.
- **Malen nach Zahlen**: Bilder, die durch das Lösen von Rechenaufgaben Feld für Feld
  entsprechend der Lösungszahl ausgemalt werden.
- **Uhrzeit lesen**: Analoge/digitale Uhr ablesen und einstellen üben.
- **Geld rechnen**: Mit Münzen/Scheinen rechnen (zusammenzählen, Wechselgeld).
- **Textaufgaben/Sachaufgaben**: Einfache, altersgerechte Textaufgaben statt reiner
  Zahlenaufgaben.
- **Mengen vergleichen**: Wie „Zahlen vergleichen", aber mit Mengen statt Zahlen.
- **Einmaleins-Trainer**: Eigener Modus, um gezielt einzelne Einmaleins-Reihen
  (z. B. nur die 7er-Reihe) zu üben statt gemischter Multiplikationsaufgaben.
- **Maßeinheiten umrechnen**: cm/m, g/kg, ml/l – eher für 3./4. Klasse.
- **Runden und Schätzen**: Zahlen auf Zehner/Hunderter runden, Ergebnisse schätzen.

## Motivation
- **Weitere Meilensteine**: Neben den Abzeichen je Fortschrittskennung etwa die Zahl aller
  gelösten Aufgaben.
- **Kopfrechnen gegen die Zeit**: Optionaler Zeitdruck-Modus für ältere/geübtere
  Kinder (abschaltbar, damit jüngere Kinder nicht gestresst werden).

## Elternbereich
- **Export der Fortschrittsdaten für Menschen**: zusätzlich als PDF/CSV aufbereitet, um sie
  mit Lehrkräften zu teilen (die vorhandene Sicherung ist ein technisches Format).
- **Erinnerungsfunktion**: Optionale, dezente Erinnerung an regelmäßiges Üben.
- **Link zur Roadmap im Elternbereich**: Ein Link auf **diese Datei** im Repo genügt – keine
  Liste in der App selbst.
  *Begründung:* Die App hat bewusst keine einzige Android-Berechtigung. Eine eingebaute
  Liste müsste fest im Code stehen und bräuchte für jede Ideenänderung einen signierten
  Release; sie nachzuladen würde eine Internet-Berechtigung in einer Kinder-App bedeuten.
  Ein Link öffnet den Browser und umgeht beides. Diese Datei bleibt die einzige Quelle.

## Zugänglichkeit & Komfort
- **Mehr vorlesen**: Heute liest das Maskottchen vor allem Inseln, Übungsnamen und
  Begrüßungen vor (`Narrator`).
  Ausbaufähig auf die Aufgaben selbst und weitere Texte.
- **Manueller Dunkelmodus-Schalter**: Unabhängig von der Systemeinstellung
  (aktuell nutzt die App immer das helle Pergament-Schema, unabhängig vom System – siehe
  `docs/adr/0009-nur-ein-farbschema-ohne-dunkelmodus.md`; ein Umschalter bräuchte ein zweites,
  eigenständig gestaltetes Thema samt Grafiken).

## Adaptivität
- **Automatische Schwierigkeitsanpassung**: Zahlenraum/Komplexität passt sich
  schrittweise an die Erfolgsquote an, ergänzend zum gezielten Wiederholen
  falsch gelöster Aufgaben.

## Aufräumen
- **Begriffe im Code an `CONTEXT.md` angleichen**: Der Code benutzt an einigen Stellen noch
  andere Wörter als das Glossar – „Game" für die Runde, „Rechenspiel"/„Zahlenspiel" für die
  Übungsform, `ranges` für etwas, das bei Zahlenmauer und Verdoppeln eine Schwierigkeitsstufe
  ist. Die Abweichungen sind in `docs/agents/domain.md` einzeln aufgeführt.
  Die persistierten Namen sind entkoppelt
  (`docs/adr/0005-persistierte-namen-entkoppelt.md`), ein Umbenennen kostet also keinem Kind
  seinen Fortschritt. Bleibt ein Refactoring mit voller Test-Pflicht.
