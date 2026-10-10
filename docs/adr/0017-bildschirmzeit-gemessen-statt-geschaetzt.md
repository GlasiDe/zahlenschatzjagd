# Bildschirmzeit wird gemessen, nicht aus der Historie geschätzt

Die Bildschirmzeit bekommt eine **eigene Tabelle**: je zusammenhängendem Zeitabschnitt eine
Zeile mit Profil, Beginn und Ende. Sie wird im Vordergrund gemessen und endet nach **zwei
Minuten ohne Eingabe** rückwirkend zum letzten Tipp.

Die naheliegende Alternative wäre gewesen, sie aus den Zeitstempeln der Historie zu schätzen —
das hätte kein neues Feld gebraucht und rückwirkend für die gesamte vorhandene Historie
Zahlen geliefert. Sie unterschlägt aber alles, was keine beantwortete Aufgabe ist: den Weg
über die Startkarte, das Zusammenstellen einer Runde, das Bestaunen der Schatztruhen. Eine
reine Lebenszyklus-Messung ohne Inaktivitäts-Stopp wiederum misst das Tablet, das aufgeklappt
auf dem Sofa liegt, während das Kind Kakao holt.

## Consequences

- **Die Messung beginnt mit dem Update.** Für die Zeit davor gibt es keine Zahlen und kann es
  keine geben — bewusst in Kauf genommen.
- **Ein Profilwechsel schneidet den Zeitabschnitt durch.** Die Zeit gehört dem Profil, das sie
  verbraucht hat. Zeit ohne aktives Profil und Zeit im Elternbereich wird verworfen, sonst
  misst die Auswertung die Erwachsenen und wächst bei jedem Hineinschauen.
- **Neue Tabelle heißt echte Room-Migration**, siehe
  [0004](./0004-echte-datenbank-migration-statt-zuruecksetzen.md).
- **Die Sicherung wird erweitert**, siehe
  [0003](./0003-sicherung-eigenes-format-und-zusammenfuehren.md). Zeitabschnitte werden über
  Profil und Beginn zusammengeführt; sie sind unveränderlich, ein zweites Einspielen derselben
  Datei ändert also nichts. Ohne das stünde die Bildschirmzeit nach jedem Gerätewechsel wieder
  auf null — gerade dann, wenn sie interessant würde.
- **Bleibt der Profilwechsel ohne PIN erlaubt** (die Voreinstellung), können sich Zeiten
  zwischen Geschwistern vermischen. Der Übungskalender weist darauf hin, solange das so
  eingestellt ist.
