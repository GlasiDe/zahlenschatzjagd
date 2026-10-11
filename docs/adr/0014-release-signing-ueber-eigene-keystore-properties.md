# Release-Signing über eine eigene keystore.properties

Für die Weitergabe außerhalb des Play Stores gibt es einen signierten `release`-Build. Die
Signatur-Angaben stehen in einer nicht versionierten `keystore.properties` im Projekt-Root —
bewusst **nicht** in `local.properties`, die laut eigenem Kopfkommentar von Android Studio
überschrieben werden kann. Der Keystore liegt unter `keystore/zahlenschatzjagd-release.jks`
(Alias `zahlenschatzjagd`, Zertifikat `CN=Christian Glashagen, C=DE`), ebenfalls ungetrackt.

## Consequences

- Fehlen beide Dateien (etwa auf einem anderen Rechner), bleibt der Release-Build ohne
  Signatur: `assembleDebug` funktioniert weiter, `assembleRelease` erzeugt eine unsignierte
  und damit nicht installierbare APK, bricht aber nicht ab. Genau diese APK bauen die
  GitHub-Action und F-Droid, um sie mit der signierten auf Bitgleichheit zu vergleichen.
- `gradlew assembleRelease` liefert **eine** vollständige Datei unter
  `app/build/outputs/apk/release/`. Das ist der Unterschied zum Weiterreichen einer aus
  Android Studio deployten App: Die besteht aus Split-APKs und ist so nicht installierbar.
- **Der Schlüssel ist unersetzlich.** Jedes Update muss mit demselben Schlüssel signiert sein,
  sonst lässt Android es nur nach einer Deinstallation zu – und mit ihr geht der Fortschritt.
  Er wird deshalb samt Passwort außerhalb des Rechners gesichert.
- **Der Schlüssel gilt ab 2.0.** Mit der neuen `applicationId`
  ([0032](./0032-neue-application-id-vor-der-veroeffentlichung.md)) mussten ohnehin alle Geräte
  neu installieren; ein neuer Schlüssel kostete also nichts.
