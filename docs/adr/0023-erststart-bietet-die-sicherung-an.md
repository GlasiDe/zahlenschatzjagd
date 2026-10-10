# Der Erststart bietet die Sicherung an, statt ein Profil zu erzwingen

Die Sicherung hat genau einen Moment, für den sie gebaut ist: Gerätewechsel und
Neuinstallation. In genau diesem Moment bot die App sie nicht an. Fand die Startkarte keine
Profile, schob sie auf den Profilbildschirm mit geöffnetem Anlege-Dialog; der Elternbereich mit
„Sicherung einspielen" war zwar von der Startkarte aus erreichbar, aber die Startkarte hielt man
ohne Profil keine Sekunde.

Ein Elternteil mit der Sicherungsdatei in der Hand musste also erst ein Wegwerf-Profil anlegen.
Nahm es dabei den Namen des Kindes — was naheliegt —, traf das beim Einspielen auf das
gesicherte Profil gleichen Namens: Der Abgleich läuft über den Namen, ein vorhandenes Profil
bleibt unangetastet (`docs/adr/0002-profil-kopplung-ueber-den-namen.md`), **Avatar und Freigaben
aus der Sicherung waren damit verloren**.

Der Profilbildschirm ohne Profil zeigt deshalb **zwei** Wege: ein Profil anlegen, oder eine
Sicherung einspielen.

**Beide stehen ohne Tor da**, mit derselben Begründung, die für das Anlegen schon galt
(`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`): Es gibt zu diesem Zeitpunkt weder ein
Profil zu schützen noch einen anderen Weg hinein. Die Ausnahme endet, sobald es etwas zu
schützen gibt — also mit dem ersten Profil, gleich ob angelegt oder eingespielt.

**Der Anlege-Dialog geht nicht mehr von selbst auf.** Er verdeckte sonst genau den zweiten Weg,
den es hier zu zeigen gilt.

## Considered Options

- **Das Einspielen kopieren statt umziehen.** Verworfen: Die drei Fehlerzweige (Datei zu neu,
  unlesbar, kein Zugriff) stünden doppelt, und genau dort verrottet Doppeltes.
- **Ein Tor vor den zweiten Weg.** Es gibt nichts zu schützen: Wer die App gerade installiert
  hat, kommt auch über „Neues Profil" an jeden Zustand, den er über die Sicherung erreichen
  könnte. Ein Tor kostete hier nur den Elternteil Zeit, den es angeht.
- **Nach dem Einspielen direkt zur Startkarte.** Verworfen, siehe unten: Das aktive Profil steht
  bewusst nicht in der Sicherung.
- **Nach dem Einspielen automatisch das erste eingespielte Profil aktivieren.** Bei einem Kind
  richtig geraten, bei vieren Zufall. Nach einem Gerätewechsel muss ohnehin jemand sagen, wer
  jetzt spielt.
- **Die Regel aus `BackupMerge.kt` umdrehen**, sodass ein eingespieltes Profil Avatar und
  Freigaben des vorhandenen überschreibt. Verworfen: Die Regel ist richtig — sie verhindert,
  dass eine alte Datei aktuelle Einstellungen überfährt. Sie schlug hier nur deshalb ungünstig
  aus, weil der Weg vorher durch ein Wegwerf-Profil führte.

## Consequences

- **Die Startkarte fragt nach, wenn kein Profil aktiv ist**, nicht mehr nur, wenn es gar keine
  gibt. Ohne passendes Profil liefert `freigabeVon` „alles offen"; nach dem Einspielen stünden
  sonst alle Inseln offen, obwohl die eingespielten Freigaben etwas anderes sagen. Eine
  Freigabe, die erst einmal nicht gilt, ist genau die Art stiller Fehler, die hier nichts zu
  suchen hat. Der Fall tritt praktisch nur nach dem Einspielen auf: Anlegen, Umbenennen und
  Löschen ziehen den aktiven Namen jeweils mit.
- **Die Zeile „Es war bereits alles vorhanden" gilt jetzt nur noch für ihren eigenen Fall.** Auf
  einem frisch installierten Gerät war sie falsch, wenn die Datei schlicht nichts enthielt.
  Beide Fälle lassen sich ohne Änderung der Signatur unterscheiden: Kam nichts an **und** war
  auch nichts zu überspringen, war die Datei leer.
- **Die Sicherung ist aus `ui/parent/` nach `ui/sicherung/` umgezogen** — Bildschirm, ViewModel
  und Meldungstexte. Sie gehört keinem der beiden Bildschirme, die sie benutzen. Der Erststart
  bekommt „Sicherung erstellen" dabei mitgeliefert und ruft es nicht auf: totes Gewicht, aber
  keine Gefahr, und billiger als eine zweite Form. In `ui/parent/` bleibt, was wirklich nur
  Eltern angeht.
- **Die Klassen behalten ihr `Backup…` im Namen.** Sie auf `Sicherung…` umzubenennen wäre
  richtig gedacht, fasst aber rund zehn Dateien an und gehört nicht als Beifang in eine
  Fehlerbehebung; die Übersetzungstabelle in `docs/agents/domain.md` führt den Begriff.
- **Wer trotzdem zuerst ein Profil anlegt und *danach* eine Sicherung mit demselben Namen
  einspielt, verliert daraus Avatar und Freigaben.** Das bleibt so. Mit dem zweiten Weg läuft
  kaum noch jemand hinein.
