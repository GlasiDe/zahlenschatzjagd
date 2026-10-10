# Profilverwaltung hinter dem Tor, Wechseln bleibt frei

Das Profil-Abzeichen der Startkarte führte in eine Profilverwaltung, in der jedes Kind mit zwei
Tippern das Profil seines Geschwisterkindes **löschen** konnte — samt dessen gesamtem
Fortschritt. Seit die Freigabe am Profil hängt
(`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`), ist das Löschen zusätzlich der
bequemste Weg, eine Sperre loszuwerden.

Anlegen, Umbenennen, Avatar ändern und Löschen liegen deshalb hinter demselben Tor wie der
Elternbereich: der Elternfrage, oder der PIN, sobald eine gesetzt ist
(`docs/adr/0016-pin-optional-neben-der-elternfrage.md`). **Das Wechseln bleibt frei** — das ist
der Alltagsvorgang, wenn Geschwister sich am Gerät abwechseln, und es macht nichts kaputt.

Wer das Tor passiert, darf danach alles. Ein zweites Mal zu fragen wäre Schikane: Wer die
Elternfrage gelöst oder die PIN eingegeben hat, ist erwachsen — dafür gibt es keine zweite
Stufe.

## Considered Options

- **Alles hinter das Tor, auch das Wechseln.** Verworfen als Voreinstellung: Dann müsste ein
  Kind, das nur an sein eigenes Profil will, jedes Mal einen Erwachsenen holen. Als
  **Schalter** gibt es diese Variante trotzdem — siehe unten.
- **Nur das Löschen schützen.** Zu wenig: Umbenennen trifft denselben Fortschritt (der Name ist
  der Schlüssel, `docs/adr/0002-profil-kopplung-ueber-den-namen.md`), und ein neu angelegtes
  Profil ist ein Profil ohne Freigabe.
- **Ein eigenes, schwächeres Tor vor der Profilverwaltung.** Ein zweites Geheimnis im Haushalt,
  das dieselbe Frage anders beantwortet — mehr zu merken, nicht mehr Schutz.
- **Die Verwaltung ganz in den Elternbereich verlegen** und den Bildschirm auf das Wechseln
  eindampfen. Verworfen: Dann läge dasselbe an zwei Stellen, und der Weg zum Anlegen führte
  über einen Bildschirm, auf dem es nichts zu suchen hat.

## Consequences

- **Ein Schalter „Profilwechsel ohne PIN erlauben", pro Gerät, voreingestellt an.** Ein Update
  darf keine Funktion wegnehmen, die vorher da war; sonst müsste nach dem Update jedes Kind auf
  jedem bestehenden Gerät erst die Elternfrage lösen, um an sein eigenes Profil zu kommen. Wer
  ihn ausschaltet, schiebt das Tor auch vor das Wechseln.
- **Steht der Schalter an, kann ein Kind auf ein fremdes Profil wechseln.** Übungszeit und
  Fortschritt landen dann beim falschen Kind. Der Schalter sagt das an Ort und Stelle, und der
  Übungskalender weist zusätzlich darauf hin, solange es so eingestellt ist — dort ist die falsch
  zugeordnete Zeit zu sehen, nicht hier.
- **Der Schalter liegt beim Zugang** — er ist keine Eltern-Funktion neben den anderen, sondern
  gehört zur Tür, wie die PIN selbst. Der Zugang war dafür bisher ein Textblock **über** der
  Liste und kein Eintrag: Unten unter fünf Einträgen findet ihn niemand, der nicht ohnehin
  danach sucht. Mit dem Schalter wuchs der Block auf 43 % der Bildschirmhöhe (gemessen bei
  Anzeigegröße „groß"), und von der Liste blieb beim Öffnen nur der obere Rand des ersten
  Eintrags übrig. Der Zugang ist deshalb jetzt ein **eigener Eintrag an erster Stelle**: Dort
  trägt er dieselbe Sorge und lässt die Übersicht wieder eine Übersicht sein. Auch der Weg nach
  dem Vergeben einer PIN endet dort — für beide Wege gleich, den aus dem Elternbereich wie den
  über die Sicherheitsfrage.
- **Er ist gerätegebunden und steht nicht in der Sicherung**, aus demselben Grund wie die PIN:
  Auf einem neuen Gerät gilt zunächst wieder die Voreinstellung.
- **Die Freischaltung steht im Navigationspfad, nicht in einem gemerkten Zustand.** Ein Zurück
  auf die Startkarte nimmt sie damit mit, statt sie liegen zu lassen — wer erneut verwalten
  will, geht erneut durch das Tor.
- **Die Frischinstallation legt weiter ohne Tor an.** Es gibt dort kein Profil zu schützen und
  keinen anderen Weg hinein; die App bestünde sonst aus diesem einen gesperrten Bildschirm.
  Aus demselben Grund steht dort inzwischen auch das Einspielen einer Sicherung ohne Tor
  (`docs/adr/0023-erststart-bietet-die-sicherung-an.md`).
- **Das Tor kennt jetzt zwei Ziele.** Die Prüfung bleibt eine einzige Stelle; nur der Weg
  dahinter unterscheidet sich. Der Rückweg bei vergessener PIN endet weiterhin beim Vergeben
  einer neuen und damit im Elternbereich, ganz gleich, wozu das Tor aufgemacht wurde.
