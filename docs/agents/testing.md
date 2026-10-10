# Tests

Wie in diesem Projekt getestet wird. Die verbindliche Test-Pflicht steht in `CLAUDE.md`;
hier steht, **wie** sie erfüllt wird und welche Fallstricke es gibt.

## Der Schnitt: was ohne Gerät entscheidbar ist, läuft ohne Gerät

- **Unit-Tests** (`app/src/test/`, `gradlew test`) decken alles ab, was ohne Android
  entscheidbar ist: Aufgaben-Generierung, Abzeichen-Logik, ViewModels, das Zusammenführen
  von Sicherungen, das Dateiformat, Rückmeldetexte, Größenberechnung.
- **Instrumentierte Tests** (`app/src/androidTest/`, `gradlew connectedAndroidTest`) decken
  ab, was ein Gerät braucht: Room-DAOs samt Migration, die Sicherung gegen eine echte
  Datenbank, und je Übungsform einen Weg von der Startkarte bis zur Auswertung.

**Der Schnitt bestimmt, wo Code liegt.** Reine Logik liegt außerhalb der UI-Pakete und ist
frei von Compose und Android — das ist der Grund, warum Dateien wie `BackupMerge.kt` oder
`ParentAccess.kt` nicht in der UI-Schicht liegen, obwohl sie dorthin zu gehören scheinen.
Ausschlaggebend ist, **wer sie braucht**: Sobald die Datenschicht einen Typ kennen muss, gehört
er nach `data/` — andernfalls zeigt der Pfeil verkehrt herum. `BackupMessage.kt` liegt deshalb
neben der Oberfläche in `ui/sicherung/`: Es ist zwar ebenso Compose-frei, wird aber nur von der
Oberfläche gelesen.
Ebenso kennt der Elternbereich weder Datei noch Datenbank: Lesen und Schreiben kommen als
Lambda herein, die Datenbank steckt hinter einer Schnittstelle.

Determinismus kommt über festen Zufallsgenerator, einen gemeinsamen Test-Scheduler für
Coroutinen und In-Memory-Attrappen der DAOs.

**Bewusst nicht getestet:** das hörbare Sprechen der Vorlesefunktion, das tatsächliche
Abspielen von Tönen (beides gerätespezifisch), der Android-Dateidialog beim Sichern und die
eingeblendete Tastatur (beides gehört dem System). Beim letzten Punkt ist wenigstens die
Voraussetzung festgehalten: `TastaturInsetsTest` prüft die `adjustResize`-Angabe im Manifest,
ohne die das `imePadding()` im `ZahlenschatzjagdNavHost` auf Android vor 11 wirkungslos wäre.

## Regeln für neue Tests

- **Flow-Tests brauchen ein Profil.** In einem `@Before` sicherstellen, dass ein aktives
  Profil existiert — sonst schiebt sich der Anlage-Dialog der Frischinstallation in die
  Assertions.
- **Zustand vor dem ersten Frame ändern?** Dann die Activity direkt starten statt über die
  Compose-Testregel — sonst ist die Oberfläche schon gezeichnet, bevor das Aufräumen greift.
- **Auf den DataStore wartet keine virtuelle Zeit.** Er antwortet in echter Zeit, also bringt
  `advanceUntilIdle` einen davon abhängigen Zustand nicht voran. Auf die erste echte Antwort
  warten (`first { … }`). Hängt der Zustand zusätzlich an `stateIn(…, WhileSubscribed)`, muss
  im Test jemand mitlesen — ohne Sammler bleibt es beim Startwert.
- **Persistiertes Format wird gegen von Hand geschriebene Daten geprüft**, nie im
  Reihum-Verfahren. Ein Reihum-Test bemerkt eine Umbenennung nicht, weil Schreiber und Leser
  gemeinsam mitwandern. Gilt für das Sicherungsformat wie für die Fortschrittskennungen —
  siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`.
- **Eine neue Datenart in der Sicherung braucht Testdaten.** `SicherungVollstaendigkeitTest`
  fährt einen Rundlauf — sichern, auf ein leeres Gerät einspielen, erneut sichern, vergleichen —
  und liest die zu prüfenden Felder aus dem Serialisierungs-Bauplan von `BackupFile`, nennt also
  keine Datenart beim Namen. Wer ein Feld der Sicherungsdatei ergänzt, befüllt es dort in
  `fuelleTestdaten` — oder trägt es mit Begründung in die Ausnahmeliste ein. Er läuft unter
  `connectedAndroidTest` und greift damit erst am Gerät, nicht im Pre-Commit-Tor.
  **Er ersetzt die Regel darüber nicht**, sondern steht daneben: Er prüft *Deckung* — kommt jede
  Datenart wieder heraus —, nicht die Namen, unter denen sie in der Datei steht. Weil er reihum
  fährt, wanderte eine Umbenennung in ihm unbemerkt mit; dagegen steht weiterhin allein
  `BackupSerializationTest` mit seinen von Hand geschriebenen Golden Files.
- **Der Emulator läuft mit Anzeigegröße „groß"** (Dichte 540). Das ist Absicht: Genau dort
  traten Layout-Fehler auf. Bei unerwartet roten UI-Tests zuerst `adb shell wm density`
  prüfen.
- **Doku-Verweise** auf Kotlin-Dateien sichert `DocsReferenceTest` ab: Nennt ein Dokument
  eine Datei, die es nicht mehr gibt, wird der Test rot.

## Instrumentierte Tests laufen **nie**, während ein echtes Gerät an adb hängt

`connectedAndroidTest` geht auf **alle** verbundenen Geräte. Auf einem Gerät, auf dem die App
benutzt wird, liegt der **signierte Release-Build**, die Test-App ist ein Debug-Build mit anderer Signatur — und Gradle
löst das, indem es die vorhandene App **deinstalliert**. Mit ihr geht die Datenbank.

Das hat schon einmal den Fortschritt gekostet: Ein Gerät war per WLAN gekoppelt, damit die neue
Version daraufkonnte, und der nächste `connectedAndroidTest`-Lauf räumte die App
ab. Ein zweiter Rettungsweg existiert nicht — die App trägt `allowBackup=false`
(`docs/adr/0022-kein-android-auto-backup.md`), es gibt also nur die von Hand angelegte
**Sicherung**.

`.claude/hooks/geraete-schutz.ps1` blockiert den Lauf deshalb, sobald ein Gerät verbunden ist,
das kein Emulator ist. Wer trotzdem auf einem echten Gerät testen muss, trennt es (`adb
disconnect …`) oder nagelt den Lauf fest (`ANDROID_SERIAL=emulator-5554`).

**Zum Aufspielen ist ohnehin ein anderer Befehl gemeint**: `adb -s <gerät> install -r <apk>`
ersetzt, ohne zu löschen (siehe `docs/agents/release.md`).

## Fallstricke, die schon einmal zugeschlagen haben

Diese Tests existieren, weil der Fehler real aufgetreten ist:

- **Abzeichen mit Unterstrich im Namen gingen verloren.** Die Freischaltung wird als
  Navigations-Argument übergeben; das Trennzeichen war früher der Unterstrich, den
  Kennungen wie `VERLIEBTE_ZAHLEN` selbst enthalten.
- **Die Vorlesefunktion verstummte auf zwei Wegen.** Text, der während des Starts der
  Sprach-Engine ankam, wurde verworfen statt gemerkt. Und eine gestartete Engine galt als
  unbrauchbar, wenn keine deutsche Stimme installiert war — statt mit der vorhandenen zu
  sprechen.
- **Bei Anzeigegröße „groß" waren zwei Bildschirme unbedienbar.** Der Start-Button rutschte
  aus dem Bild, die Auswertungs-Buttons wurden auf Höhe null gedrückt. Deshalb liegen die
  Aktions-Buttons der Auswertung heute fest verankert am unteren Rand, während der Teil
  darüber scrollt — der Abzeichen-Block kann beliebig hoch werden.
- **Das Farbschema fiel versehentlich auf Blau/Weiß zurück.** Ein Guard prüft die
  Pergament-Töne (siehe `docs/adr/0009-nur-ein-farbschema-ohne-dunkelmodus.md`).
- **Die Zahlenmauer wertete Folgefehler doppelt** (siehe
  `docs/adr/0006-zahlenmauer-mit-eigenem-bildschirm.md`).
- **Ton und Begrüßung kamen nach dem Drehen doppelt.** Das ViewModel übersteht das Drehen,
  die Composition nicht – ein nackter `LaunchedEffect` spielte denselben Ton noch einmal.
  `EinmaligeAusgabenInstrumentedTest` dreht deshalb echt (`ActivityScenario.recreate()`) und zählt
  über einen Beobachter in `SoundPlayer` und `Narrator` mit; der Beobachter zählt die Aufrufe,
  nicht den Klang. Die Regel selbst steht in
  `docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`.
- **Beim Update ging Fortschritt verloren.** Die Migration wird deshalb gegen eine von Hand
  aufgebaute Alt-Datenbank gefahren, nicht gegen ein exportiertes Schema.
- **Nach einem Android-Studio-Update waren elf DataStore-Tests rot** („Unable to rename
  …preferences_pb.tmp"). Android Studio Rabbit brachte Java 25 mit; weil der Gradle-Daemon Java 21
  verlangt, lud Gradle selbst ein Temurin 21 – und dessen `File.renameTo` kann unter Windows keine
  bestehende Datei ersetzen, das JetBrains Runtime kann es. `gradle/gradle-daemon-jvm.properties`
  verlangt deshalb ausdrücklich **JetBrains** als Hersteller. Wer die Datei mit `updateDaemonJvm`
  neu erzeugt, gibt `--jvm-vendor=jetbrains` mit.
