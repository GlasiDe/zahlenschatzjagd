# Ton und Vorlesefunktion hängen am Gerät, nicht am Profil

Alles, was sich in dieser App einstellen lässt, hängt am Profil: die Freigabe
(`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`), der Fortschritt, die gemessene
Bildschirmzeit (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`). Die beiden Schalter
für **Ton** und **Vorlesefunktion** tun das nicht — sie gelten für das Gerät.

Der Grund steht in der Frage, die sie beantworten: **„Ton aus" ist eine Aussage über den Ort**,
nicht über das Kind. Man legt den Schalter im Wartezimmer um, im Auto, in der Schule — und dreht
ihn zu Hause wieder auf. Am Profil festgemacht müsste man ihn beim Betreten des Wartezimmers für
jedes Geschwisterkind einzeln ausschalten und danach für jedes einzeln wieder ein; und das Kind,
das als nächstes das Gerät nimmt, brächte den Ton ungefragt zurück.

Für die Vorlesefunktion ließe sich das Gegenteil vertreten — ob ein Kind vorgelesen bekommt,
klingt nach einer Eigenschaft des Kindes. Beide Schalter stehen trotzdem gemeinsam am Gerät: Sie
liegen auf einem Bildschirm, beantworten dieselbe Frage („was kommt hier gerade aus dem Gerät?")
und wären als Paar mit zwei verschiedenen Geltungsbereichen die verwirrendste aller Varianten.
Wird das Vorlesen eines Tages wirklich zur Eigenschaft eines Kindes, ist das ein eigener Schnitt
mit eigener Begründung.

Der Ausnahmecharakter ist damit ausdrücklich: **Diese beiden Schalter sind die einzige
Abweichung.** Wer eine neue Einstellung baut, hängt sie ans Profil, solange nicht dasselbe
Ort-Argument trägt.

## Considered Options

- **Am Profil, wie alles andere.** Verworfen aus dem Grund oben: Der Schalter beschreibt dann das
  falsche Ding, und in einem Haushalt mit mehreren Kindern wird aus einem Handgriff einer pro
  Kind.
- **Ton am Gerät, Vorlesen am Profil.** Der ehrlichste Kompromiss, und trotzdem verworfen: Zwei
  benachbarte Schalter, die unterschiedlich weit reichen, sind an der Oberfläche nicht erklärbar,
  ohne dass der Bildschirm zu einem Merkzettel wird.
- **Kein Schalter, stattdessen die Lautstärketasten des Geräts.** Deckt den Ton ab, aber nicht die
  Vorlesefunktion — und wer stumm stellt, verliert auch alles andere auf dem Gerät. Für „leise
  üben, aber Ella soll noch vorlesen" gibt es dann keinen Weg.

## Consequences

- **Beide sind voreingestellt an.** Ein Update darf nichts wegnehmen, was vorher da war; auf jedem
  bestehenden Gerät bleibt nach dem Update alles, wie es klang.
- **Sie stehen nicht in der Sicherung**, wie die PIN
  (`docs/adr/0016-pin-optional-neben-der-elternfrage.md`) und der Profilwechsel-Schalter
  (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`). Die Sicherung trägt den Fortschritt eines
  Kindes auf ein neues Gerät; die Lautstärke des alten gehört nicht dazu. Dort gilt zunächst
  wieder die Voreinstellung.
- **Der Schalter sitzt im `SoundPlayer` bzw. im `Narrator`, nicht an den Aufrufstellen.** Beide
  sind prozessweite Singletons, die ohnehin jeder Ton und jeder vorgelesene Satz passiert. Die
  Alternative — jeder Bildschirm liest den Schalter selbst — hätte ihn an acht Stellen verlangt,
  und der erste, der ihn vergisst, macht ihn wirkungslos. Gesetzt werden beide an genau einer
  Stelle, dem Sammler an der Wurzel des `ZahlenschatzjagdNavHost`.
- **Der Vorleser kennt einen dritten Zustand: „Schalter noch unbekannt".** Die Begrüßung der
  Startkarte wird im ersten Frame losgeschickt, die Antwort des DataStore kommt später. Würde bis
  dahin „an" angenommen, spräche ein ausgeschalteter Vorleser bei jedem App-Start genau einmal.
  Unbekannt heißt deshalb vormerken — dieselbe Behandlung, die ein Text während der noch
  startenden Sprach-Engine erfährt. Beim Ton ist das nicht nötig: Der erste Ton kommt frühestens
  nach einer gelösten Aufgabe.
- **Ausschalten bricht einen laufenden Satz ab.** Wer den Schalter umlegt, will Ruhe, nicht das
  Ende des angefangenen Textes.
