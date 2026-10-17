# Release

Verteilt wird die App als signierte APK über die GitHub-Releases dieses Repos und über F-Droid.
Jedes Release trägt einen Git-Tag, damit sich zu jeder APK genau der Quelltext finden lässt, aus
dem sie gebaut ist. F-Droid baut aus diesem Tag nach und liefert unsere signierte APK nur aus,
wenn sein Build bis auf die Signatur bitgleich ist
(`docs/adr/0033-f-droid-mit-signatur-des-entwicklers.md`).

## Ablauf

1. **Version erhöhen:** `versionCode` und `versionName` in `app/build.gradle.kts`. Der
   Versionsname steht über `BuildConfig.VERSION_NAME` in jeder Sicherungsdatei, eine verteilte
   Fassung muss also unterscheidbar bleiben. Dazu der **Änderungshinweis**
   `fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt`: höchstens 500 Zeichen (Grenze
   von F-Droid), in wenigen Sätzen, was sich für Kinder und Eltern ändert. Er ist zugleich die
   Grundlage der Notizen im GitHub-Release.

   **Zeigt die App ihn auch?** Wer seine Version in `aenderungshinweiseInDerApp` (oben in
   `app/build.gradle.kts`, `versionCode to versionName`) einträgt, bekommt den Hinweis nach dem
   Update einmal beim Start eingeblendet – F-Droid aktualisiert automatisch, die Eltern sähen ihn
   sonst nie (#8). Einträge bleiben stehen; wer Versionen überspringt, bekommt alle dazwischen.
   - **Pflicht**, sobald eine Version eine neue Übungsform oder einen neuen Zahlenraum bringt:
     Neues ist für bestehende Profile sofort freigegeben
     (`docs/adr/0018-freigabe-als-sperre-gespeichert.md`). Der Hinweis nennt die Neuerung dann
     beim Namen. Wo sie sich abwählen lässt, schreibt der Dialog selbst darunter – das gehört
     nicht in den Text.
   - **Nicht** bei reinen Wartungsversionen („Für Kinder und Eltern ändert sich nichts"): Die
     stören beim Start nur.

   Ist eine Version markiert, ohne dass es ihren Text gibt, bricht der Build ab.
2. **Committen.** Gebaut wird immer aus einem Commit, nie aus einem Arbeitsstand.
3. **Lokal signiert bauen:** `gradlew assembleRelease`. Die Signatur kommt aus der nicht
   versionierten `keystore.properties`
   (`docs/adr/0014-release-signing-ueber-eigene-keystore-properties.md`). **Nie in der CI**:
   Der Schlüssel verlässt den Rechner des Entwicklers nicht.
4. **Ablegen:** die APK aus `app/build/outputs/apk/release/` nach `releases/` kopieren, benannt
   `Zahlenschatzjagd-<versionName>-vc<versionCode>-<datum>.apk`. Der Ordner ist ignoriert und
   liegt bewusst außerhalb von `app/build/`, weil `gradlew clean` dort alles löscht.
5. **Auf einem Gerät prüfen:** `adb -s <gerät> install -r <apk>`. Das ersetzt, ohne zu löschen,
   solange die Signatur dieselbe ist. **Nie deinstallieren** und kein `connectedAndroidTest`
   gegen ein Gerät mit Release-Build (`docs/agents/testing.md`): Mit der App geht die
   Datenbank.
6. **Pushen und auf die Action warten:** `git push origin main` (nach Rückfrage, siehe
   `CLAUDE.md`), dann `gh run list --commit <sha>` bzw. `gh run watch <id> --exit-status`, bis
   der Lauf für genau diesen Commit grün ist. Bei Rot wird nicht getaggt: erst die Ursache
   klären. Liegt sie im App-Code, ist das ein neuer Stand derselben Nummer – neu committen,
   bauen und prüfen.
7. **Auf Bitgleichheit prüfen:** Die Action legt eine unsignierte Release-APK aus einem
   Linux-Build ab (7 Tage aufbewahrt). Herunterladen und mit der eigenen vergleichen:

   ```
   gh run download <id> -n app-release-unsigned -D build/abgleich
   docker run --rm -v "${PWD}:/r" debian:trixie-slim sh -c "apt-get update -qq && apt-get install -y -qq --no-install-recommends apksigcopier apksigner > /dev/null 2>&1 && apksigcopier compare /r/releases/<apk> --unsigned /r/build/abgleich/app-release-unsigned.apk && echo BITGLEICH"
   ```

   Ohne `BITGLEICH` wird nicht getaggt: F-Droid würde diese Fassung nicht bauen. Die Ursache
   findet `diffoscope` (Debian-Paket `diffoscope-minimal`) zwischen beiden APKs. Wer an der
   Ursache dreht, hält sich an ADR-0033.
8. **Taggen:** `git tag -a v<versionName> -m "<versionName> (vc <versionCode>)"`, dann
   `git push origin v<versionName>`.
9. **Veröffentlichen:** die APK aus Schritt 4 als `Zahlenschatzjagd-<versionName>.apk`
   kopieren – **ohne Datum**, denn F-Droid sucht sie über das Muster
   `Zahlenschatzjagd-%v.apk` – und
   `gh release create v<versionName> <apk> --title "<versionName>" --notes-file <notizen.md>`.
   Die Notizen sind der Änderungshinweis aus Schritt 1 plus die SHA-256-Prüfsumme der APK.

F-Droid bemerkt den neuen Tag von selbst und baut nach. Bis die Fassung dort erscheint, vergehen
erfahrungsgemäß einige Tage.

Tag und Release schreiben eine Fassung öffentlich fest, deshalb kommen sie zuletzt: erst wenn
Gerät, Action **und** Bitgleichheit zugestimmt haben. Wann gebaut wird, ist dagegen gleich – die
APK hängt am Commit, nicht am Push. Ein Commit auf `main` ohne Tag ist noch kein Release.

## Eine Nummer darf neu gebaut werden, solange sie nicht veröffentlicht ist

Zeigt die Prüfung auf dem Gerät noch einen Fehler, geht die Korrektur in dieselbe Nummer: neu
committen, neu bauen, neu prüfen. Getaggt wird erst der Stand, der die Prüfung bestanden hat. Ist
ein Release veröffentlicht, ist seine Nummer eingefroren; was danach kommt, bekommt eine neue.
