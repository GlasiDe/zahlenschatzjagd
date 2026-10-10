# Der Ertrag einer Zahlenmauer ist gestaffelt und für alle Stufen gleich hoch

Eine Zahlenmauer bleibt **eine** Aufgabe in der Historie, trägt aber nicht mehr nur 1 oder 0 zu
ihrer Truhe bei, sondern einen gestaffelten **Ertrag** (`CONTEXT.md`):

| Stufe  | Lücken | ganz richtig | teilweise richtig | sonst |
|--------|--------|--------------|-------------------|-------|
| Leicht | 3      | 2            | ab 2 richtigen: 1 | 0     |
| Mittel | 6      | 2            | ab 4 richtigen: 1 | 0     |
| Schwer | 10     | 2            | ab 6 richtigen: 1 | 0     |
| Ultra  | 10     | 2            | ab 6 richtigen: 1 | 0     |

„Richtig" heißt dabei, was das Prüfen der Mauer schon heute darunter versteht: Eine Lücke wird
gegen die Eingaben darunter gerechnet, ein Folgefehler zählt nicht doppelt.

Anlass: Eine Mauer zählte so viel wie eine einzelne Plusaufgabe und nur, wenn jede
Lücke stimmte. Die Truhen der Zahlenmauer füllten sich dadurch deutlich langsamer als alle
anderen, und eine fast fehlerfreie Mauer brachte so wenig wie eine leere.

## Considered Options

- **Ertrag je richtiger Lücke** (3 / 6 / 10 / 10 für eine ganze Mauer). Rechnerisch am fairsten
  gegenüber den Grundrechenarten, aber jede Stufe hat ihre eigene Truhe – und die schweren
  wären in wenigen Runden voll. Eine Truhe, die man nach zehn Runden Ultra bis Platin gefüllt
  hat, entwertet gerade die anspruchsvollste Stufe.
- **Jede Lücke als eigene Aufgabe in der Historie.** Ergibt Teilerfolg von selbst, ändert aber,
  was eine Zeile der Zahlenmauer bedeutet: Sterne und Übungsstand zählten dann Lücken statt
  Mauern.
- **Nur eigene Schwellen der Stufen** (Bronze früher). Füllt schneller, würdigt Teilerfolg aber
  nicht.

Gewählt ist eine bewusst **flache** Staffel: Sie entschärft das Truhenproblem, ohne es
umzukehren. Eine Mauer ist anspruchsvoller als eine einzelne Aufgabe, und es ist in Ordnung, dass
ihre Truhen länger brauchen.

## Consequences

- **Der Ertrag braucht einen eigenen Platz in der Historie**, weil „teilweise richtig" sich aus
  `isCorrect` nicht ablesen lässt. Das heißt Datenbank-Migration und ein neues Feld in der
  Sicherung (`docs/adr/0004-echte-datenbank-migration-statt-zuruecksetzen.md`,
  `docs/adr/0005-persistierte-namen-entkoppelt.md`).
- **Rückwirkend nur nach oben.** Bereits gespeicherte ganz richtige Mauern bekommen den vollen
  Ertrag 2; bereits gespeicherte falsche bleiben bei 0. Sie lassen sich nicht verlässlich
  nachrechnen: Gespeichert sind Lösung und Eingabe je Lücke, nicht aber die Zellen, gegen die
  damals geprüft wurde. Da Abzeichen aus der Historie abgeleitet werden
  (`docs/adr/0008-abzeichen-aus-der-historie-abgeleitet.md`), springen bestehende Truhen mit dem
  Update einmalig nach oben – still, ohne Meldung in einer Auswertung. Weggenommen wird nichts.
- **Sterne und Übungsstand bleiben unberührt.** Sie zählen weiter ganze, ganz richtige Mauern;
  der Ertrag wirkt allein auf die Truhen.
- **Weder Kind noch Eltern sehen den Ertrag.** Die Truhe zeigt ohnehin keine Einheit, nur den
  Abstand zum nächsten Schatz.
- Der Ton nach dem Prüfen folgt **nicht** dem Ertrag: „richtig" nur für eine ganz richtige Mauer,
  sonst „falsch" – wie bei Nachbarzahlen.
