# Domain Docs

Wie die Engineering-Skills die Domänen-Dokumentation dieses Repos konsumieren sollen.

## Vor dem Erkunden lesen

- **`CONTEXT.md`** im Repo-Root — die Begriffe des Projekts.
- **`docs/adr/`** — die ADRs, die den Bereich berühren, in dem gearbeitet wird.

Dies ist ein **Single-Context-Repo**: ein `CONTEXT.md` und ein `docs/adr/` im Root, kein
`CONTEXT-MAP.md`.

## Die Begriffe des Glossars verwenden

Wo eine Ausgabe einen Domänenbegriff benennt — Issue-Titel, Refactoring-Vorschlag,
Hypothese, Testname —, gilt der Begriff aus `CONTEXT.md`. Die dort unter `_Avoid_`
gelisteten Synonyme werden nicht verwendet.

Fehlt ein benötigter Begriff im Glossar, ist das ein Signal: Entweder wird Sprache erfunden,
die das Projekt gar nicht benutzt (dann überdenken), oder es gibt eine echte Lücke (dann für
`/domain-modeling` vormerken).

## Der Code weicht an bekannten Stellen ab

Der Code ist älter als das Glossar und benutzt stellenweise noch andere Wörter. Das ist
bekannt und benannt, kein Versehen: In Prosa und in **neuen** Bezeichnern gilt das Glossar,
beim Lesen bestehenden Codes braucht es diese Übersetzung:

| Im Code | Meint laut `CONTEXT.md` | Anmerkung |
|---|---|---|
| `Game…` (`GameScreen`, `GameViewModel`, `GameSettings`, `GameUiState`) | **Runde** | „Game" meint hier nie eine Übungsform |
| `Rechenspiel`, `Zahlenspiel`, `…Games` | **Übungsform** | zwei Typen für denselben Begriff, je Insel einer |
| `taskCategory`, `BadgeCategory.key` | **Fortschrittskennung** | mischt Rechenart, Übungsform und Schwierigkeitsstufe |
| `GameSettings.types` | **Rechenart** (als Liste) | trägt Auswahl-Beschriftungen, keine Kennungen |
| `showCoins` | **Hilfe** | wählt bei Verliebte Zahlen die Schwierigkeitsstufe „Mit Hilfe" und damit ihre Kennung |
| `BadgeCategory.ranges` | **Zahlenraum** – bei Zahlenmauer und Verdoppeln aber **Schwierigkeitsstufe** | dort steht ein fester Wert je Stufe, keine Wahl des Kindes |
| `gesperrteKennungen`, `gesperrteZahlenraeume` | **Freigabe** – umgekehrt | gespeichert wird das Abgewählte, siehe `docs/adr/0018-freigabe-als-sperre-gespeichert.md` |
| `userName`, „Nutzer" | **Profil** | |
| `AdultGate`, `AdultChallenge`, `adult_gate_*` | **Elternfrage** | die Datei ist älter als das Glossar; neue Bezeichner heißen `Parent…` |
| `SoundPlayer`, `GameSound` | **Ton** | `CONTEXT.md` rät von „Sound" ab; die beiden Namen sind älter als der Glossareintrag |
| `Backup…` (`BackupFile`, `BackupStore`, `BackupScreen`, `BackupMessage`) | **Sicherung** | `CONTEXT.md` rät von „Backup" ausdrücklich ab; das Angleichen fasst rund zehn Dateien an und wartet auf ein eigenes Vorhaben |

Das Angleichen steht als eigenes Vorhaben in `ROADMAP.md`. Es ist ungefährlich, weil die
persistierten Namen entkoppelt sind
(`docs/adr/0005-persistierte-namen-entkoppelt.md`) — bleibt aber ein Refactoring mit voller
Test-Pflicht, kein Nebenbei-Umbenennen.

## Widersprüche zu ADRs benennen

Widerspricht eine Ausgabe einem bestehenden ADR, wird das ausdrücklich gesagt, statt die
Entscheidung still zu übergehen:

> _Widerspricht ADR-0002 (Profil-Kopplung über den Namen) — aber einen zweiten Blick wert,
> weil …_

Besonders empfindlich ist `docs/adr/0005-persistierte-namen-entkoppelt.md`: Was dort als
eingefroren gilt, kostet bei Missachtung Kinder ihren Fortschritt.
