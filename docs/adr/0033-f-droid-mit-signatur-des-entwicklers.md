# F-Droid verteilt die APK mit der Signatur des Entwicklers

Die App kommt zusätzlich über F-Droid, und zwar als **dieselbe** App wie auf GitHub. F-Droid baut
jedes Release aus dem Git-Tag nach. Ist dieser Build bis auf die Signatur bitgleich mit der APK
aus den GitHub-Releases, liefert F-Droid die vom Entwickler signierte APK aus statt einer selbst
signierten. Wer die App von GitHub hat, kann dann über F-Droid aktualisieren und umgekehrt, ohne
den Fortschritt über die **Sicherung** umziehen zu müssen.

Signiert F-Droid selbst, entstehen zwei Apps mit derselben `applicationId` und verschiedenen
Signaturen. Android lässt zwischen ihnen keinen Wechsel zu, ohne die App zu löschen – und mit ihr
den Fortschritt.

## Considered Options

- **Nur GitHub-Releases.** Jede neue Fassung muss als APK gesucht und mit freigegebenen
  „unbekannten Quellen" installiert werden. Genau diese Hürde soll F-Droid senken: Dort wird
  einmal pro Gerät der Client installiert, danach kommen Updates mit einem Antippen. Verworfen.
- **F-Droid signiert selbst.** Kein Nachweis der Bitgleichheit nötig, aber zwei unvereinbare
  Ausgaben. Bleibt der Rückfall, falls die Bitgleichheit eines Tages nicht mehr gelingt – dann
  mit einem deutlichen Hinweis im README, dass ein Wechsel zwischen den Bezugsquellen über die
  Sicherung läuft.
- **IzzyOnDroid**, ein zweites Repo für den F-Droid-Client, das die APK direkt aus den
  GitHub-Releases nimmt. Es behält die Signatur, ohne nachzubauen. Verworfen, weil Eltern das
  Repo erst von Hand im Client eintragen müssten – eine weitere Hürde statt einer kleineren. Auch
  als Rückfall nicht vorgesehen: Lieber F-Droid mit eigener Signatur.
- **F-Droid mit der Signatur des Entwicklers** (gewählt). Nachgewiesen am 11.10.2026: Ein Build
  unter Windows und einer unter Linux (Docker, anderer Pfad) waren bis auf die Signatur
  bitgleich (`apksigcopier compare`). Der Gradle-Daemon lief beide Male auf JetBrains JDK 21,
  jeweils in der Fassung für die eigene Plattform.

## Consequences

- **Jedes Release muss bitgleich nachbaubar sein.** Weicht eine Fassung ab, baut F-Droid sie
  nicht, und die Nutzer dort bleiben auf der alten. Deshalb baut die GitHub-Action bei jedem Push
  eine unsignierte Release-APK unter Linux, und vor jedem Tag wird sie mit der lokal signierten
  verglichen (`docs/agents/release.md`).
- **Was die Bitgleichheit trägt:** LF-Zeilenenden auch in Windows-Arbeitskopien
  (`.gitattributes`), kein Abhängigkeits-Blob im Signaturblock (`dependenciesInfo`), ein
  Release-Build, der ohne Schlüssel unsigniert durchläuft
  ([0014](./0014-release-signing-ueber-eigene-keystore-properties.md)). Baseline-Profil und die
  Versionskontroll-Angaben in der APK stören nicht, solange aus einem Commit gebaut wird.
- **Nichts in den Build, was nicht aus dem Commit folgt:** keine Bauzeit, keine Pfade, kein
  „dirty"-Kennzeichen. Das betrifft etwa den geplanten Git-Stand in App und Sicherung (#2).
- **Die Release-Datei heißt `Zahlenschatzjagd-<versionName>.apk`**, ohne Datum. F-Droid findet
  sie über ein Muster, das nur Versionsname und `versionCode` kennt.
- **Der Rückweg ist teuer.** Wechselt F-Droid später auf die eigene Signatur, sind alle
  F-Droid-Installationen von den GitHub-Installationen getrennt und müssen über die Sicherung
  umziehen. Der Schlüssel ab 2.0 bleibt damit für beide Bezugsquellen unersetzlich.
- **Erste Fassung auf F-Droid ist 2.0.2.** 2.0 und 2.0.1 tragen noch den Abhängigkeits-Blob.
