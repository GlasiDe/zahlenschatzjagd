# Release

Verteilt wird die App als signierte APK über die GitHub-Releases dieses Repos. Jedes Release
trägt einen Git-Tag, damit sich zu jeder APK genau der Quelltext finden lässt, aus dem sie
gebaut ist.

## Ablauf

1. **Version erhöhen:** `versionCode` und `versionName` in `app/build.gradle.kts`. Der
   Versionsname steht über `BuildConfig.VERSION_NAME` in jeder Sicherungsdatei, eine verteilte
   Fassung muss also unterscheidbar bleiben.
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
7. **Taggen:** `git tag -a v<versionName> -m "<versionName> (vc <versionCode>)"`, dann
   `git push origin v<versionName>`.
8. **Veröffentlichen:** `gh release create v<versionName> <apk> --title "<versionName>"
   --notes-file <notizen.md>`. Die Notizen sagen in wenigen Sätzen, was sich für Kinder und
   Eltern ändert, und nennen die SHA-256-Prüfsumme der APK.

Tag und Release schreiben eine Fassung öffentlich fest, deshalb kommen sie zuletzt: erst wenn
Gerät **und** Action zugestimmt haben. Wann gebaut wird, ist dagegen gleich – die APK hängt am
Commit, nicht am Push. Ein Commit auf `main` ohne Tag ist noch kein Release.

## Eine Nummer darf neu gebaut werden, solange sie nicht veröffentlicht ist

Zeigt die Prüfung auf dem Gerät noch einen Fehler, geht die Korrektur in dieselbe Nummer: neu
committen, neu bauen, neu prüfen. Getaggt wird erst der Stand, der die Prüfung bestanden hat. Ist
ein Release veröffentlicht, ist seine Nummer eingefroren; was danach kommt, bekommt eine neue.
