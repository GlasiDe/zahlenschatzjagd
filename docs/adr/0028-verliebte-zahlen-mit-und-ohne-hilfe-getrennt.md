# Verliebte Zahlen „mit Hilfe" und „ohne Hilfe" sind zwei Stufen mit eigener Kennung

Die Münzen bei Verliebte Zahlen machen aus einer Übungsform zwei **Schwierigkeitsstufen**:
„Mit Hilfe" und „Ohne Hilfe". Jede hat ihre eigene Fortschrittskennung, ihre eigene Truhe und
ihr eigenes Kästchen in der Freigabe – wie die Stufen von Verdoppeln, Zahlenmauer und
Nachbarzahlen.

Die bestehende Kennung `VERLIEBTE_ZAHLEN` bedeutet von nun an **„mit Hilfe"**; für „ohne Hilfe"
kommt eine neue Kennung dazu. Die Kennung selbst bleibt unverändert
(`docs/adr/0005-persistierte-namen-entkoppelt.md`), nur ihre Bedeutung wird enger.

Anlass: Eltern konnten die Münzen nicht abwählen. Das Kind schaltete sie selbst ein und aus,
und die Freigabe kannte nur Kennungen und Zahlenräume.

## Considered Options

- **Die Hilfe als dritter Teil der Freigabe**, die Übung bleibt eine Kennung mit einer Truhe.
  Das entspricht der Lesart von **Hilfe** in `CONTEXT.md`, verlangt aber eine neue Spalte am
  Profil (Migration und Sicherungsfeld) und einen Sonderfall im Freigabe-Bildschirm. Außerdem
  mischt der Übungsstand dann weiter Aufgaben mit und ohne Münzen in einer Trefferquote.
- **Zwei Stufen** (gewählt). Das Glossar lässt es ausdrücklich zu: Eine Schwierigkeitsstufe
  kann sich dadurch unterscheiden, wie viel Hilfe angeboten wird. Eine neue Kennung ist dank
  `docs/adr/0018-freigabe-als-sperre-gespeichert.md` für jedes bestehende Profil von selbst
  freigegeben – es braucht keine neue Spalte, nur die kleine Datenmigration aus den
  Consequences. Und Eltern sehen im Übungsstand, ob ihr Kind es
  schon ohne Münzen kann.

## Consequences

- **Der bisherige Fortschritt zählt vollständig für „mit Hilfe".** Die Historie hat nie
  festgehalten, ob mit Münzen geübt wurde; eine Aufteilung ist unmöglich. „Mit Hilfe" war die
  Voreinstellung des Schalters, also liegt dort der größere Teil. Wer schon viel ohne Münzen
  geübt hat, findet diesen Fortschritt in der Truhe „Mit Hilfe" – verloren geht nichts. Die Truhe
  „Ohne Hilfe" beginnt leer.
- **Die Beschriftung heißt „Mit Hilfe" / „Ohne Hilfe"**, für das Kind wie in der Freigabe – und
  nicht „Leicht" / „Schwer" und nicht „mit Münzen". „Münzen" versteht ohne Erklärung weder Kind
  noch Erwachsener, und dasselbe Wort an beiden Stellen hält Auswahl und Freigabe zusammen.
- **Ist nur eine der beiden Stufen freigegeben, sieht das Kind keinen Schalter** und übt in der
  freigegebenen. Damit können Eltern die Hilfe auch erzwingen; das ist eine Entscheidung der
  Eltern, nicht der App.
- **Wer Verliebte Zahlen vorher abgewählt hatte, behält das.** Die neue Kennung wäre nach
  ADR-0018 von selbst freigegeben, die Übungsform käme also über „Ohne Hilfe" zurück. Die
  Migration 7 → 8 sperrt sie deshalb überall mit, wo `VERLIEBTE_ZAHLEN` gesperrt war. Das gilt für
  jede Datenbank, die das Update durchläuft.
- **Eine Lücke bleibt bewusst offen: alte Sicherungen.** Eine Sicherungsdatei aus der Zeit vor
  dieser Entscheidung trägt dieselbe Zeichenkette `VERLIEBTE_ZAHLEN`, meinte damit aber die ganze
  Übungsform; eine neue meint nur „Mit Hilfe". Die Datei verrät nicht, welche Bedeutung gilt.
  Spielt man eine **alte** Sicherung ein und legt sie dabei ein Profil **neu** an – neues Gerät,
  Neuinstallation –, steht bei diesem Kind „Ohne Hilfe" wieder offen. Ein schon vorhandenes Profil
  ist nicht betroffen, weil das Einspielen seine Freigabe nicht anfasst (`BackupMerge.kt`).
  Abgewogen wurde ein Kennzeichen in der Sicherungsdatei, an dem alte Dateien erkennbar wären.
  Es hätte die Sicherung zu einer zweiten Stelle gemacht, die bei jeder künftigen Aufteilung einer
  Kennung mitgepflegt werden muss – gegen einen Fall, der selten ist, sichtbar und harmlos ausgeht
  (das Kind übt eine Stufe, die die Eltern nicht wollten), mit einem Kästchen in der Freigabe
  behoben ist und mit dem Alter der Sicherungen verschwindet. Stört die Lücke doch, lässt sich das
  Kennzeichen nachrüsten, solange es noch Dateien ohne es gibt.
