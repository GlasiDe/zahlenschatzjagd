# Getrennte Dateien je Insel und Übungsform statt gemeinsamer Abstraktion

Rechnen-Insel und Zahlenspiele-Insel haben je ein eigenes Spiel-Modell und einen eigenen
Menü-Bildschirm, obwohl beide fast gleich aussehen. Ebenso hat jede Übungsform ihren eigenen
Auswahl-Bildschirm. Die Doppelung ist gewollt.

Eine gemeinsame Abstraktion würde die Inseln aneinander binden, obwohl sie sich
unterschiedlich entwickeln sollen — und die Auswahl-Bildschirme unterscheiden sich real:
Zahlenraum, Schwierigkeitsstufe, Hilfe-Umschalter und Aufgabenzahl kommen in wechselnden
Kombinationen vor.

## Consequences

Wer die Doppelung sieht, wird sie „aufräumen" wollen. Vor einem solchen Refactoring gehört
geprüft, ob die betroffenen Bildschirme sich seither wirklich angeglichen haben — sonst
entsteht eine Abstraktion mit Sonderfällen, die schwerer zu lesen ist als die Kopie.
