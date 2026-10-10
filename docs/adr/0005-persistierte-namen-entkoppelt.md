# Persistierte Namen sind von Code-Namen entkoppelt

Jede Property, deren Name in einer Datenbank-Spalte oder einem Feld der Sicherungsdatei landet,
trägt diesen Namen ausdrücklich als Annotation (`@ColumnInfo` bzw. `@SerialName`). Der
Kotlin-Name und der gespeicherte Name sind damit zwei verschiedene Dinge, auch wo sie heute
gleich lauten.

Anlass ist die Begriffsarbeit in `CONTEXT.md`: Der Code soll seine Bezeichner an die geklärte
Sprache angleichen können. Ohne diese Trennung wäre jede Umbenennung zugleich eine
Formatänderung — und da die App als Release-Build auf den Geräten läuft, an deren Daten
niemand von außen herankommt, hieße das verlorener Fortschritt.

## Was daraus folgt

- **Umbenennen im Code ist folgenlos** für Datenbank und Sicherungsdatei, solange die
  Annotation stehen bleibt.
- **Die gespeicherten Namen sind eingefroren.** Zwei Tests halten sie fest: eine von Hand
  geschriebene Sicherungsdatei, die weiterhin lesbar sein muss, und eine Prüfung, dass der
  Export dieselben Feldnamen schreibt. Beide sind bewusst **nicht** aus den Datenklassen
  abgeleitet — ein Reihum-Test bemerkt eine Umbenennung nicht, weil Schreiber und Leser
  gemeinsam mitwandern.
- **Die Fortschrittskennungen** (`"PLUS"`, `"ZAHLENMAUER_ULTRA"` …) sind ebenso eingefroren und
  durch einen eigenen Test gegen eine handgepflegte Liste abgesichert. Sie zu ändern hieße,
  Abzeichen still auf leer zurückzusetzen: Der Fortschritt läge dann unter einer Kennung, die
  niemand mehr zählt.

Neue Kennungen dürfen jederzeit dazukommen. Bestehende zu ändern ist eine Migration, keine
Umbenennung.
