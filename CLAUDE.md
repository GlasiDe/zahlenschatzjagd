# CLAUDE.md

## Projekt-Dokumentation

- **`CONTEXT.md`** – die Begriffe des Projekts. **Lies es, bevor du neue Klassen, Felder oder
  Bildschirme benennst**, und benutze die dortigen Wörter auch in Antworten und Kommentaren.
- **`docs/adr/`** – warum einzelne Dinge so entschieden wurden. **Lies den passenden Eintrag,
  bevor du eine bestehende Entscheidung umkehrst**, und lege einen neuen an, wenn eine
  Entscheidung schwer umkehrbar ist, ohne Kontext überrascht und echte Alternativen hatte.
- **`docs/agents/`** – Arbeitsregeln: `domain.md` (wie Glossar und ADRs zu nutzen sind, plus
  die Übersetzungstabelle für abweichende Code-Begriffe), `testing.md` (**lies es, bevor du
  Tests schreibst**), `issue-tracker.md`, `release.md` (**lies es, bevor du ein Release baust**).
- **`LEHRPLAN.md`** – verbindliche Fachvorgabe aus dem NRW-Lehrplan. **Lies und befolge es vor
  Änderungen an der Aufgaben-Generierung** oder beim Ergänzen neuer Aufgabentypen und
  Zahlenräume.
- **`ASSETS.md`** – verbindliche Vorlage für Grafiken, Farben und Schrift samt Prompts und
  Seeds, dazu Herkunft und Lizenz der Töne. **Lies und befolge es vor dem Erzeugen neuer
  Bild-Assets**, vor neuen Tönen oder Änderungen am Theme.
- **`LICENSE.md`** – welcher Teil unter welcher Lizenz steht. Neue fremde Dateien (Schrift,
  Symbol, Ton) tragen ihre Lizenz dort ein.
- **`README.md`** – Überblick für Menschen: was die App ist, Installation, Datenschutz, Bauen.
- **`ROADMAP.md`** – Ideensammlung ohne Reihenfolge oder Zusage.

Ändern sich Funktionsumfang oder Begriffe, zieh `README.md` und `CONTEXT.md` im selben Zug
nach.

Die Wahl der Plattform steht in `docs/adr/0001-native-android-statt-kotlin-multiplatform.md`.

## Agent skills

### Issue tracker

Issues und Specs leben als GitHub-Issues unter `GlasiDe/zahlenschatzjagd`; `ROADMAP.md` bleibt
davon getrennt die unverbindliche Ideensammlung. Siehe `docs/agents/issue-tracker.md`.

### Triage labels

Die fünf kanonischen Rollen unter ihren eigenen Namen. Siehe `docs/agents/triage-labels.md`.

### Domain docs

Single-Context: `CONTEXT.md` und `docs/adr/` im Repo-Root. Siehe `docs/agents/domain.md`.

## Git-Workflow

- Es gibt einen Branch: `main`. Releases tragen einen Tag (`docs/agents/release.md`).
- **Keine Pull Requests für eigene Arbeit.** Eigene Arbeit geht direkt als Commit auf `main`.
  Issues tragen dabei nur ihre Kategorie (`bug`/`enhancement`) und bleiben offen, bis die
  geänderte App **auf einem Gerät** geprüft ist. Ein Pull Request von außen trägt
  `ready-for-human` im üblichen Sinn: fertig gebaut, wartet auf Prüfung und Merge.
- Frag vor **jedem** `git push` beim Nutzer nach und warte seine Bestätigung ab, auch wenn für
  andere Aktionen Freigaben erteilt wurden.
- Gib bei `git push` immer Remote und Branch an: `git push origin main`. **Niemals ein bloßes
  `git push`** – sonst geht womöglich ein Wegwerf-Branch (etwa aus `/prototype`) mit.

## Test-Pflicht

Ein Feature gilt als fertig, wenn diese vier Punkte erfüllt sind:

1. **Vorher**: `gradlew test` läuft grün.
2. **Danach**: Für die neue Funktionalität existieren Unit-Tests, bei UI-Abläufen zusätzlich
   instrumentierte Tests.
3. **Grün**: `gradlew test`, `gradlew detekt` und – sofern die Änderung Datenbank oder
   UI berührt – `gradlew connectedAndroidTest` laufen grün.
4. **Zum Schluss, vor dem Commit**: Eine Code-Prüfung (`/mattpocock-skills:code-review` seit
   dem Stand vor dem Feature, Spec ist das Issue) läuft, ohne dass der Nutzer sie anstößt. Fertig
   ist sie, wenn jeder Befund behoben oder mit Begründung verworfen ist. Sie kommt nach Punkt 3,
   damit sie den endgültigen Code sieht und nicht einen, der danach noch wegen Lint umgebaut
   wird.

Auf GitHub fährt eine Action `gradlew test detekt` bei jedem Push und Pull Request. Sie ersetzt
Punkt 3 nicht: `connectedAndroidTest` läuft dort nicht.

### Das Modell der Code-Prüfung

Lagert eine Code-Prüfung Arbeit in Unteragenten aus – gleich wie viele, gleich wie sie heißen,
gleich aus welchem Skill sie stammen –, laufen diese auf **Sonnet** statt auf dem Modell des
Hauptagenten.

Der Grund liegt in der Art der Arbeit und überlebt damit auch einen Umbau der Prüfung selbst: Sie
hält Text gegen Text – den Diff gegen das Ticket, den Diff gegen die dokumentierten Regeln dieses
Repos – und lebt vom Abgleich, nicht vom Schließen über mehrere Ecken. Was ein größeres Modell
zusätzlich findet, sind erfahrungsgemäß Ermessensfragen, die anschließend ohnehin verworfen
werden. Ein übersehener Befund kostet außerdem einen Nachzug, keinen kaputten Code: Die harten
Tore sind `gradlew test`, `detekt` und `connectedAndroidTest`, und die hängen an keinem
Prüfmodell.

Zwei Grenzen der Regel:

- **Wer Opus will, sagt es beim Anstoßen der Prüfung.** Angebracht ist das bei ungewöhnlich
  verzahnten Änderungen – etwas, das Datenhaltung, Navigation und Oberfläche in einem Zug umbaut.
  Ohne diese Ansage bleibt es bei Sonnet.
- **Für die Cloud-Prüfung (`/code-review ultra`) gilt sie nicht.** Die wählt ihr Modell selbst und
  wird ohnehin vom Nutzer ausgelöst.

### Das Tor vor dem Commit

`.claude/hooks/pre-commit-check.ps1` fährt `gradlew test detekt` unmittelbar vor jedem
`git commit` auf `main`, sobald im Turn eine `.kt`-Datei angefasst wurde, und verhindert
den Commit bei Rot. Er ist das **Netz**, nicht der Plan: Wer Punkt 3 einhält, merkt nie, dass es
ihn gibt. Verdrahtet ist er in `.claude/settings.json`, zusammen mit dem `Stop`-Hook, der
dasselbe für Turns ohne Commit nachholt.

Schlägt er trotzdem zu, hängt das weitere Vorgehen an der Art des Befunds:

- **Lint** (detekt): Die Korrektur ändert kein Verhalten, die vorhandenen Tests decken sie
  weiter ab. Beheben, erneut prüfen, committen.
- **Fehlgeschlagener Test**: Das ist eine Verhaltensänderung und schuldet die volle Schleife –
  zurück in die betroffene Test-Scheibe, und den berührten Teil noch einmal reviewen. Nicht
  eng patchen, bis der Hook schweigt.

Was er **nicht** abdeckt: `connectedAndroidTest` (zu langsam für einen Hook) und die Frage, ob
überhaupt ein Review gelaufen ist. Das bleibt Punkt 3 und 4 dieser Liste.

Wie in diesem Projekt getestet wird und welche Fallstricke bereits zugeschlagen haben, steht
in `docs/agents/testing.md`.

**Persistierte Namen und Fortschrittskennungen sind eingefroren.** Wer Felder umbenennt, hält
den gespeicherten Namen per `@ColumnInfo`/`@SerialName` fest – sonst verlieren Kinder ihren
Fortschritt. Details in `docs/adr/0005-persistierte-namen-entkoppelt.md`.

### Kein instrumentierter Lauf, solange ein echtes Gerät an adb hängt

`connectedAndroidTest` geht auf **alle** verbundenen Geräte und **deinstalliert** dort eine App
mit abweichender Signatur — auf einem Gerät mit installiertem Release-Build kostet das den
Fortschritt aller Kinder darauf. Das ist schon einmal passiert. `.claude/hooks/geraete-schutz.ps1`
blockiert es deshalb; Einzelheiten und die beiden Auswege stehen in `docs/agents/testing.md`.

Zum **Aufspielen** einer neuen Version ist nicht dieser Befehl gemeint, sondern
`adb -s <gerät> install -r <apk>` — er ersetzt, ohne zu löschen (`docs/agents/release.md`).

### Emulator für instrumentierte Tests

Ist kein Gerät verbunden (`adb devices` bleibt leer), starte selbstständig einen vorhandenen
AVD-Emulator – dafür braucht es keine Rückfrage:

```
$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe -list-avds
$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe -avd <NAME> -no-snapshot-save
```

Danach auf `sys.boot_completed` == 1 warten. Der Emulator darf anschließend weiterlaufen. Er
läuft absichtlich mit Anzeigegröße „groß" (Dichte 540), denn dort traten Layout-Fehler auf –
prüfe bei unerwartet roten UI-Tests zuerst `adb shell wm density`.
