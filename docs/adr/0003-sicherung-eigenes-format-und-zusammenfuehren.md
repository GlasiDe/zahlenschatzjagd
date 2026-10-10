# Sicherung: eigenes Dateiformat, Einspielen führt zusammen

Die Sicherung verwendet eigene Datenklassen statt der Datenbank-Entitäten, und beim Einspielen
wird der vorhandene Bestand **zusammengeführt statt ersetzt**.

## Warum ein eigenes Format

Die technische Zeilennummer der Datenbank darf nicht mitwandern — sie wird beim Einspielen neu
vergeben, sonst kollidieren Einträge. Darüber hinaus entkoppelt ein eigenes Format die Datei
von künftigen Schema-Änderungen: Die Datenbank darf sich weiterentwickeln, ohne dass Dateien
auf den Geräten der Familien unlesbar werden. Eine Formatversion in der Datei erlaubt spätere
Änderungen; eine Datei aus einer neueren App-Version wird erkannt und abgelehnt, statt falsch
gedeutet zu werden.

## Warum zusammenführen statt ersetzen

Ersetzen würde beim ersten Fehlgriff den gesamten Fortschritt eines Kindes löschen — und die
Datei ist genau für den Moment gedacht, in dem es keine zweite Kopie mehr gibt. Zusammenführen
macht das Einspielen **wiederholbar**: Dieselbe Datei ein zweites Mal einzuspielen ändert
nichts. Profile werden über den Namen abgeglichen, Aufgaben gelten als identisch, wenn Kind,
Zeitstempel, Aufgabentext und gegebene Antwort übereinstimmen. Zeitabschnitte der
Bildschirmzeit werden über Profil und Beginn abgeglichen — sie sind unveränderlich, mehr
braucht ihr Fingerabdruck nicht ([0017](./0017-bildschirmzeit-gemessen-statt-geschaetzt.md)).

## Consequences

Ein Kind kann Fortschritt nicht durch einen versehentlichen Import verlieren — wohl aber
Einträge behalten, die es lieber los wäre. Ein „Zurücksetzen" gibt es bewusst nicht.
