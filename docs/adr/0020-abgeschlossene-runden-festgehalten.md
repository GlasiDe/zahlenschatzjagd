# Abgeschlossene Runden werden festgehalten, nicht aus der Historie erschlossen

Der Übungskalender färbt einen Tag, sobald an ihm eine **Runde abgeschlossen** wurde. Dieses
Ereignis bekommt eine **eigene Tabelle**: je beendeter Runde eine Zeile mit Profil und Zeitpunkt.
Geschrieben wird sie am Ende der Runde, dort wo auch die Abzeichen fallen.

Naheliegend wäre gewesen, das aus der Historie zu lesen — sie hat ja Zeitstempel und hätte
rückwirkend für den gesamten vorhandenen Bestand Zahlen geliefert. Sie weiß aber nicht, aus wie
vielen Aufgaben eine Runde bestand: Die Anzahl wählt das Kind vorher, sie steht im
Navigationspfad und nirgends in der Datenbank. Damit ist aus der Historie eine durchgezogene Runde
nicht von einer abgebrochenen zu unterscheiden — und genau das ist hier die Frage. Die einfache
Ersatzregel „mindestens eine beantwortete Aufgabe" färbt den Tag schon, wenn ein Kind die App
öffnet und einmal danebentippt; ein Kalender, der so entsteht, sagt nichts mehr aus.

Das ist derselbe Schnitt wie bei der Bildschirmzeit
([0017](./0017-bildschirmzeit-gemessen-statt-geschaetzt.md)) und keine Umkehrung von
[0008](./0008-abzeichen-aus-der-historie-abgeleitet.md): **Fortschritt** wird weiter ausschließlich
aus der Historie abgeleitet, und die Runde trägt keinen. Was hier dazukommt, ist ein Ereignis auf
der Zeitachse, das die Historie nicht kennt.

## Consequences

- **Der Kalender beginnt mit dem Update.** Für die Zeit davor gibt es keine Punkte und kann es
  keine geben — bewusst in Kauf genommen, wie schon bei der Bildschirmzeit.
- **Nur die durchgezogene Runde zählt.** Beide Spiel-ViewModels halten sie in `finishRound` fest;
  wer über ✕ abbricht, verlässt den Bildschirm, ohne dort anzukommen. Bei der Zahlenmauer zählt
  ebenso die ganze Runde, nicht die einzelne Mauer.
- **Neue Tabelle heißt echte Room-Migration**, siehe
  [0004](./0004-echte-datenbank-migration-statt-zuruecksetzen.md).
- **Die Sicherung wird erweitert**, siehe
  [0003](./0003-sicherung-eigenes-format-und-zusammenfuehren.md). Runden werden über Profil und
  Zeitpunkt zusammengeführt; ein Eintrag ist unveränderlich, ein zweites Einspielen derselben
  Datei ändert also nichts. Ohne das stünde das Punktemuster nach jedem Gerätewechsel wieder leer,
  während die Historie mitgewandert wäre — der Widerspruch wäre schwerer zu erklären als das
  zusätzliche Feld.
- **Ein Rundenabschluss weiß nicht, *was* geübt wurde** — nur wann. Wer den Kalender später nach
  Übungsform aufschlüsseln will, braucht dafür ein Feld mehr; die Frage „was" beantwortet bis
  dahin der Übungsstand aus der Historie.
