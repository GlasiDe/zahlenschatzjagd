# Ton und Vorlesen überstehen das Drehen: Quittung, erstes Erscheinen, Besuch

Was ein Bildschirm von sich aus hören lässt, also ein **Ton** oder ein Satz der
**Vorlesefunktion**, kommt so oft, wie es gemeint ist, auch wenn der Bildschirm neu aufgebaut wird.
Ein nackter `LaunchedEffect` schafft das nicht: Dreht das Kind das Gerät, baut Android die Activity
neu, die Composition beginnt von vorn, und der Effekt spielt denselben Ton noch einmal. Das
ViewModel hat das Drehen dagegen überstanden, sein Zustand sagt also weiterhin „geprüft".
Anlass war ein Fehler: Rechnen, Zahlenmauer und Nachbarzahlen spielten den Ton zur Auswertung nach dem
Drehen ein zweites Mal, die Auswertung ihr „geschafft", Startkarte und Menüs ihre Begrüßung.

Es gibt drei Wege, und welcher gilt, hängt davon ab, woher der Anlass kommt. Alle drei liegen in
`EinmaligeAusgaben.kt`:

- **Aus dem Zustand eines ViewModels → Quittung.** Das ViewModel legt den ausstehenden Ton bzw. die
  ausstehende Ansage im Zustand ab (`ausstehenderTon`, `ausstehendeAnsage`). Die Oberfläche spielt
  sie ab und meldet `tonGespielt()` bzw. `ansageVorgelesen()`, danach steht nichts mehr aus. Beim
  Wechsel zur nächsten Aufgabe verfällt, was noch aussteht.
- **Beim ersten Erscheinen eines Bildschirms → Merker über `rememberSaveable`.** Er übersteht das
  Drehen und auch die Rückkehr aus dem Verlauf. So bleibt es bei einem „geschafft" je Runde, auch
  wenn das Kind zwischendurch die Abzeichen ansieht. Der Schutzhalt nutzt denselben Weg.
- **Bei jedem Besuch → das `Besuchsgedaechtnis` der Navigation.** Startkarte und Menüs begrüßen
  ein Kind, das zurückkommt, wieder: Das hilft beim Orientieren, wer nicht liest. Nach dem Drehen
  begrüßen sie es nicht noch einmal. Ein Besuch beginnt, wenn zuvor ein anderer Eintrag des
  Verlaufs sichtbar war. Die Navigation meldet jeden sichtbar werdenden Eintrag, und das
  Gedächtnis übersteht das Drehen über den gespeicherten Zustand.

Was auf ein Tippen hin erklingt, etwa die Insel- und Übungsnamen, braucht keinen dieser Wege.
Eine Wiederholung nach dem Tippen ist kein Fehler.

## Considered Options

- **Einmal-Ereignis über einen `Channel`.** Verworfen: Ein Ereignis,
  das gesendet wird, während niemand sammelt, geht verloren oder hängt in einem Puffer, und Google
  rät für Ereignisse aus dem ViewModel inzwischen zum Zustand mit Quittung. Bildschirme ohne
  ViewModel (Auswertung, Startkarte, Menüs) erreicht der Weg ohnehin nicht.
- **`rememberSaveable` für alles.** Verworfen für die Bildschirme mit ViewModel: Der Merker
  übersteht auch das Beenden des Prozesses, das ViewModel nicht. Danach zählt die Runde wieder von
  vorn, und ein Merker, der sich „bis zur dritten Prüfung gespielt" gemerkt hat, verschluckt die
  dritte Prüfung der neuen Runde. Außerdem ließe er sich nur instrumentiert prüfen, die Quittung
  dagegen im ViewModel-Test. Für die Begrüßung bei jedem Besuch reicht er nicht, weil er auch die
  Rückkehr aus dem Verlauf übersteht.
- **Den Merker beim Verlassen zurücksetzen**, außer bei einer Konfigurationsänderung. Damit wäre
  das Besuchsgedächtnis überflüssig. Verworfen: Das hängt an der Reihenfolge, in der Compose beim
  Verlassen aufräumt und speichert, also an einem Implementierungsdetail.
- **Die Ausrichtung festlegen.** Dann gäbe es kein Drehen mehr. Aber auch das Umschalten des
  Dunkelmodus im System, der Sprache oder der Anzeigegröße baut die Activity neu, und das Problem bliebe.

## Consequences

- **`EinmaligeAusgabenWaechterTest` hält die Regel fest.** Steht `SoundPlayer.play` oder
  `Narrator.speak` außerhalb von `EinmaligeAusgaben.kt` im Block eines `LaunchedEffect`,
  `DisposableEffect` oder `SideEffect`, wird `gradlew test` rot, und damit auch das Tor vor dem
  Commit. Er ist eine Text-Suche, keine Code-Analyse: Eine ausgefallene Schreibweise rutscht durch.
- **`EinmaligeAusgabenInstrumentedTest` dreht echt** (`ActivityScenario.recreate()`) und zählt über
  einen Beobachter in `SoundPlayer` und `Narrator`, wie oft abgespielt bzw. vorgelesen werden
  sollte. Was aus dem Lautsprecher kommt, bleibt ungeprüft (`docs/agents/testing.md`).
- **Startkarte und Menüs bekommen `begruessungFaellig` hereingereicht.** Ohne den Parameter
  begrüßt ein Bildschirm nicht. Wer einen neuen Bildschirm mit Begrüßung baut, verdrahtet ihn im
  `ZahlenschatzjagdNavHost` wie die vorhandenen.
- **Nach dem Beenden des Prozesses** steht die Runde ohnehin wieder am Anfang. Die Auswertung und
  die Startkarte bleiben dann still, weil ihr Merker überlebt hat. Das ist gewollt: Sie waren schon
  einmal zu hören.
