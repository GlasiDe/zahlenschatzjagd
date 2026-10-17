# Einmaleins mit Kennungen je Reihe und Stufe

Das Einmaleins bekommt **18 Fortschrittskennungen**: eine für jede Reihe (2 bis 10) in jeder der
beiden Schwierigkeitsstufen Aufsagen und Durcheinander, nach dem Schema `EINMALEINS_7_AUFSAGEN`
und `EINMALEINS_7_DURCHEINANDER`. Jede davon hat ihre eigene Truhe, ihre eigene Zeile im
Übungsstand und ihr eigenes Kästchen in der Freigabe.

Der Grund ist die Frage, die das Einmaleins gegenüber Mal in den Grundrechenarten überhaupt erst
beantwortet: **Welche Reihe sitzt noch nicht – und sitzt sie nur aufgesagt oder auch
durcheinander?** Das kann nur eine Kennung je Reihe und Stufe zeigen. Dazu passt die Schule, die
die Reihen nacheinander einführt: Eltern können eine Reihe sperren, die noch nicht dran war.

## Considered Options

- **Eine Kennung je Reihe** (9), beide Stufen zählen gemeinsam. Mischt eine leichte und eine
  schwere Stufe in einer Trefferquote, entgegen der Regel in `CONTEXT.md`, nach der unterschiedlich
  schwere Stufen getrennte Kennungen haben.
- **Eine Kennung je Stufe** (2). Wenige Truhen, eine kurze Freigabe – aber welche Reihe hakt, ist
  nicht mehr zu sehen, und eine einzelne Reihe lässt sich nicht sperren.
- **In die bestehende Kennung `MAL` zählen.** `MAL` wird je Zahlenraum gezählt, eine Reihe hat
  keinen; und jede Aufgabe geht mit genau einer Kennung in die Historie ein.

## Consequences

- **Die Entscheidung ist nur in eine Richtung umkehrbar.** Kennungen sind eingefroren
  (`docs/adr/0005-persistierte-namen-entkoppelt.md`). Zu feine Kennungen lassen sich in der
  Darstellung jederzeit zusammenfassen; eine zu grobe ließe sich nie wieder aufteilen, weil die
  Historie die Reihe dann nicht festgehalten hätte.
- **Der Preis liegt in der Darstellung.** Der Abzeichen-Reiter „Einmaleins" trägt 18 Truhen, die
  Freigabe 18 Kästchen. Beide zeigen sie als Raster Reihe × Stufe statt als Liste.
- **Neue Kennungen sind für bestehende Profile sofort freigegeben**
  (`docs/adr/0018-freigabe-als-sperre-gespeichert.md`). Es braucht keine Migration; ein
  Änderungshinweis beim Start sagt den Eltern, dass es die Übungsform gibt.
- **Geteilt gehört nicht dazu.** Umkehraufgaben (`56 : 7`) bleiben Sache der Grundrechenarten.
  Kämen sie doch als dritte Stufe, bekämen sie eigene Kennungen nach demselben Schema.
