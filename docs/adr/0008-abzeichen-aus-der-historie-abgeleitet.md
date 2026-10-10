# Abzeichen werden aus der Historie abgeleitet, nicht gespeichert

Es gibt keine Abzeichen-Tabelle. Der Stand wird bei Bedarf aus der Aufgaben-Historie
gezählt: Anzahl richtig gelöster Aufgaben je Fortschrittskennung und Zahlenraum, daraus die
erreichte Stufe. Sterne werden gar nicht gespeichert — sie sind reines Rundenfeedback.

So kann der angezeigte Stand nicht vom tatsächlichen Fortschritt abweichen; es gibt nur eine
Quelle der Wahrheit. Ein zweiter, gespiegelter Zählerstand wäre eine dauerhafte Fehlerquelle
und müsste bei jedem Einspielen einer Sicherung mitgepflegt werden.

## Consequences

- **Die Freischaltung während einer Runde braucht ein Extra-Verfahren.** Weil der Stand erst
  am Ende neu gezählt würde, lädt die Runde beim Start den Ausgangsstand je Kennung, zählt
  richtige Antworten runden-lokal mit und bestimmt am Ende, welche Schwellen neu
  überschritten wurden. Ohne das wäre die Freischaltung von der Reihenfolge nebenläufiger
  Datenbankschreibvorgänge abhängig.
- Kennungen zu ändern setzt Abzeichen still zurück — siehe
  [0005](./0005-persistierte-namen-entkoppelt.md).
