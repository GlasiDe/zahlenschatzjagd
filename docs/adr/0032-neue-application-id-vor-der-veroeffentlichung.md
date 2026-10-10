# Neue applicationId vor der Veröffentlichung

Die App heißt ab Version 2.0 **Zahlenschatzjagd** und bekommt die `applicationId`
`io.github.glaside.zahlenschatzjagd` statt `com.example.meinmathefreund`. Kotlin-Paket und
`namespace` ziehen mit. Für Android ist das eine **andere App**: Die alte bleibt installiert, die
neue startet leer, und der Fortschritt kommt nur über die **Sicherung** hinüber.

Der Zeitpunkt ist bewusst gewählt: kurz vor der Veröffentlichung im neuen Repo. Später wird
der Umzug nur teurer, weil dann auch fremde Geräte die App tragen, die niemand persönlich kennt.
Und `com.example.` ist der Vorgabewert von Android Studio, den der Play Store ablehnt – er stünde
sonst öffentlich fest.

## Considered Options

- **Id behalten, nur den sichtbaren Namen ändern.** Kein Umzug, aber `com.example.*` dauerhaft
  öffentlich, und der Play Store bliebe ausgeschlossen. Verworfen.
- **Später ändern.** Verworfen, weil jeder weitere Monat mehr Geräte umziehen müssten.
- **Eine eigene Domain als Präfix.** Verworfen, weil sie die App öffentlich mit einem Geschäft
  des Entwicklers verknüpfen würde. `io.github.<konto>` ist an das GitHub-Konto gebunden und die
  Form, die F-Droid für Projekte ohne eigene Domain empfiehlt.

## Consequences

- **Der Umzug läuft über die Sicherung.** Sie hängt nicht an der Id, und das Einspielen ergänzt
  nur: Hat ein Kind in der neuen App schon gespielt, findet sein **Profil** über den Namen trotzdem
  den alten Fortschritt. Wer bisher eine Version 1.x nutzt, findet die
  Kurzanleitung im Release-Text zu 2.0.
- **Die alte App erst löschen, wenn das Einspielen geprüft ist.** Beide Apps können
  nebeneinander installiert sein.
- **Die Versionsnummer springt auf 2.0**, der `versionCode` läuft weiter (16), damit
  Release-Tabelle und Sicherungen eindeutig bleiben.
- Gespeichert wird der Paketname nirgends (kein WorkManager, keine serialisierten Klassennamen),
  das Umbenennen des Pakets berührt also keine **Fortschrittskennung**
  ([0005](./0005-persistierte-namen-entkoppelt.md)).
