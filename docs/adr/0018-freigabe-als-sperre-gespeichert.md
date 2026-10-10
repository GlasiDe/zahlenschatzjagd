# Die Freigabe wird als Sperre gespeichert

Auf dem Gerät liegt nicht, was ein Profil **darf**, sondern was ihm **genommen** wurde: zwei
Listen mit abgewählten Fortschrittskennungen und abgewählten Zahlenräumen, als Spalten
`gesperrteKennungen` und `gesperrteZahlenraeume` am Profil. Die Freigabe im Sinne von
`CONTEXT.md` entsteht beim Lesen als Rest (`freigabeAusSperren` in `data/Freigabe.kt`).

Damit erfüllt sich eine Zusage aus
`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md` von selbst: **Neue Kennungen sind
für bestehende Profile freigegeben.** Eine Übungsform, die es beim Setzen der Freigabe noch
nicht gab, kann in keiner Sperre stehen — also ist sie da, sobald die App sie kennt. Der leere
Eintrag heißt „nichts abgewählt" und ist zugleich der Zustand jedes frisch angelegten Profils
und jedes Profils, das die Migration 3 → 4 vorfindet.

## Considered Options

- **Die erlaubten Mengen speichern** (das Naheliegende, deckungsgleich mit dem Begriff). Eine
  später ergänzte Übungsform wäre für alle bestehenden Kinder unsichtbar gewesen — genau das,
  was ADR-0015 ausschließt. Reparabel nur durch eine Datenmigration bei **jedem** neuen
  Eintrag in `BADGE_CATEGORIES`, also durch eine Pflicht, die niemand im Kopf behält.
- **Erlaubte Mengen plus eine Liste der damals bekannten Kennungen.** Löst dasselbe Problem,
  kostet aber ein drittes Feld, dessen einziger Zweck es ist, den Unterschied zwischen „nicht
  erlaubt" und „gab es noch nicht" nachzureichen. Die Sperre trägt diesen Unterschied ohne
  Zusatzfeld.
- **Eine eigene Tabelle je Profil und Kennung.** Wäre die sauberste Normalform, verlangt aber
  eine dritte Kaskade beim Umbenennen und Löschen von Profilen
  (`docs/adr/0002-profil-kopplung-ueber-den-namen.md`). Zwei Spalten am Profil folgen beidem
  von allein.

## Consequences

- **Der Code liest gegenläufig zum Glossar.** `CONTEXT.md` spricht von der Freigabe, die
  Spalten sprechen von der Sperre. Die Umrechnung passiert an genau einer Stelle
  (`freigabeAusSperren` / `Freigabe.gesperrteKennungen`); ab dort arbeitet alles mit der
  Freigabe. Wer eine der beiden Spalten direkt liest, muss diese Kehrtwende kennen.
- **Werte, die es nicht mehr gibt, werden still übergangen.** Eine entfallene Kennung in der
  Sperre eines alten Profils ist kein Fehler, sondern bedeutungslos.
- **Die Freigabe wandert in der Sicherung mit** und wurde dort ohne neue Formatversion
  ergänzt: Ältere Dateien kennen die Felder nicht, ihr Vorgabewert ist der leere Eintrag — und
  der heißt „nichts abgewählt" (`docs/adr/0003-sicherung-eigenes-format-und-zusammenfuehren.md`).
