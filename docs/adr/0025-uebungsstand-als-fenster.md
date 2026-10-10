# Der Übungsstand rechnet auf einem Fenster, nicht auf der ganzen Historie

Der Übungsstand zeigt eine Trefferquote. Sie wird **nicht** über alles gerechnet, was in der
Historie steht, sondern über ein Fenster: die **letzten 50 Aufgaben je Fortschrittskennung**,
zusätzlich begrenzt auf die **letzten 30 Tage**. Was außerhalb liegt, geht in keine Zahl ein.

Über die gesamte Historie gerechnet hätte die Quote zwei Eigenschaften, die sie für die Frage
„sitzt das gerade?" unbrauchbar machen. Sie wird mit der Zeit **träge**: Nach tausend Aufgaben
verschiebt eine schlechte Woche die Prozentzahl um weniger als einen Punkt, und genau diese Woche
ist der Anlass, im Elternbereich nachzusehen. Und sie wird **besser, sobald ein Kind aufhört zu
üben** — der Bestand bleibt stehen, altert aber nicht, sodass ausgerechnet der Stillstand wie ein
guter Stand aussieht. Beide Fenstergrenzen adressieren je eine Hälfte davon: Die 50 halten die Zahl
beweglich, die 30 Tage lassen sie veralten, wenn nichts nachkommt.

Die Grenzen gelten **je Fortschrittskennung** und nicht über alles zusammen. Sonst verdrängte
fleißiges Plus-Üben das Minus aus dem Fenster, und über Minus stünde nichts mehr da, obwohl es geübt
wurde.

Das ist keine Umkehrung von
[0008](./0008-abzeichen-aus-der-historie-abgeleitet.md): Die Historie bleibt die einzige Quelle, und
sie wird weiterhin vollständig aufbewahrt. Gefenstert wird erst beim **Lesen** — Abzeichen zählen
unverändert alles.

## Consequences

- **Unter einer Mindestanzahl steht keine Prozentzahl**, sondern „zu wenig geübt". Eine Quote aus
  drei Aufgaben sagt nichts, sieht aber aus, als sagte sie etwas. Die Schwelle steht auf dem
  Bildschirm, sonst liest sich der Satz wie ein Fehler.
- **Die Schwelle ist je Kennung deklariert** (`BadgeCategory.mindestAufgaben`, Standard 10), weil
  „eine Aufgabe" je Übungsform verschieden viel ist: Die Zahlenmauer schreibt eine Zeile je **Mauer**
  (3–10 je Runde), die Grundrechenarten eine je Aufgabe (5–25 je Runde). Dort liegt die Schwelle bei
  5. Eine neue Übungsform, die gröber oder feiner zählt, setzt ihre eigene.
- **Eine Zeile rechnet nur mit dem, was darunter selbst eine Prozentzahl trägt.** Nachgetragen,
  nachdem die erste Fassung am Gerät auffiel: Grundrechenarten stand auf 95 %, aufgeklappt trug
  einzig Minus eine Zahl — 96 % —, alles andere „zu wenig geübt". Die obere Zahl lag damit
  *außerhalb* des Bereichs aller sichtbaren Zahlen, weil die zu dünnen Übungen mitsummiert wurden.
  Rechnerisch war das die vollständigere Aussage; auf dem Bildschirm war es ein Widerspruch, denn
  daneben stand ausdrücklich, über diese Aufgaben lasse sich nichts sagen. **Nachvollziehbarkeit
  schlägt hier statistische Vollständigkeit** — einer Zahl, die man nicht nachrechnen kann, glaubt
  man zu Recht nicht.
  Der Preis ist ein zweiter, seltener Fall: Tragen alle Übungen einer Übungsform je zu wenig, trägt
  auch die Übungsform keine Zahl, selbst wenn in Summe genug zusammenkäme. Aufgeklappt steht dann
  aber sofort da, warum.
- **Zwei Kinder mit gleicher Quote können verschieden viel geübt haben.** Deshalb steht die Anzahl
  neben der Prozentzahl und nicht nur diese. Die Anzahl bleibt **vollständig**, auch wo die Quote
  nur einen Teil wertet: „Was geübt wurde" ist die andere Frage des Bildschirms.
- **Der Bildschirm kann etwas anderes sagen als die Abzeichen.** Ein Kind mit voller Truhe kann im
  Übungsstand „noch nicht geübt" tragen, wenn es 30 Tage pausiert hat. Das ist gewollt — die beiden
  beantworten verschiedene Fragen —, aber es ist die Stelle, an der jemand einen Fehler vermuten
  wird.
- **Die Datenbank siebt schon vor**: Gelesen wird nur der 30-Tage-Ausschnitt
  (`TaskHistoryDao.aufgabenSeit`), nicht die ganze Historie. Die 50 spannt dann die reine Logik in
  `ui/parent/Uebungsstand.kt` auf. Ohne diesen Schnitt wüchse die Abfrage mit jedem geübten Jahr.
- **Kein Ausschluss über die Freigabe.** Was gesperrt ist, verschwindet nicht aus dem Übungsstand:
  Es wurde vielleicht geübt, bevor es gesperrt wurde, und dann ist es hier eine Antwort und keine
  Zeile zu viel.
