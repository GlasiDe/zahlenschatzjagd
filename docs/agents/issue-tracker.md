# Issue tracker: GitHub

Issues und Specs dieses Repos leben als GitHub-Issues unter
`GlasiDe/zahlenschatzjagd`. Alle Operationen über die `gh`-CLI; sie erkennt das Repo
innerhalb eines Klons selbst aus `git remote -v`.

## Abgrenzung zu `ROADMAP.md`

Beide halten „was noch kommt" fest, aber auf verschiedenen Ebenen — sie ersetzen einander
nicht:

- **`ROADMAP.md`** – lose Ideensammlung ohne Reihenfolge und ohne Zusage. Vieles davon wird
  nie umgesetzt. Sie richtet sich auch an Nutzer und Mitleser, nicht nur an Entwickler.
- **GitHub-Issue** – konkrete Arbeit, die jemand angeht: mit Abhängigkeiten, Zustand und
  Zuständigkeit.

Eine Idee wandert von der Roadmap in ein Issue, **wenn sie umgesetzt werden soll**. Sie
vorher als Issue anzulegen, bläht den Tracker mit Dingen auf, die niemand vorhat.
Umgekehrt gehört nichts in die Roadmap, was bereits ein offenes Issue hat.

## Konventionen

- **Anlegen**: `gh issue create --title "..." --body "..."` (mehrzeilige Bodies per Heredoc).
- **Lesen**: `gh issue view <nummer> --comments`
- **Auflisten**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`
- **Kommentieren**: `gh issue comment <nummer> --body "..."`
- **Labels**: `gh issue edit <nummer> --add-label "..."` / `--remove-label "..."`
- **Schließen**: `gh issue close <nummer> --comment "..."`
- **Milestone**: `gh issue edit <nummer> --milestone "2.1.0"`; anlegen per
  `gh api repos/GlasiDe/zahlenschatzjagd/milestones -f title="2.1.0"`

Issue-Titel verwenden die Begriffe aus `CONTEXT.md` — „Übungsform", nicht „Spiel".

## Milestones: was in ein Release gehört

Jedes Issue, das auf `main` landet, bekommt den Milestone des nächsten Releases, benannt nach
dessen `versionName` („2.1.0“). Der Milestone hält zusammen, was ein Release enthält; die
Release-Sitzung liest ihn mit `gh issue list --milestone "2.1.0" --state all` und schreibt daraus
den Änderungshinweis (`docs/agents/release.md`).

**Geschlossen heißt erledigt, nicht veröffentlicht.** Ein Issue wird geschlossen, sobald sein
Code auf `main` liegt, die Code-Prüfung durch ist und die App auf einem Gerät geprüft wurde. Der
Grund sind die Skills: `to-tickets` arbeitet die Frontier ab („any ticket whose blockers are all
done“), `wayfinder` und GitHubs eigene Blockierung werten ein Ticket erst als frei, wenn seine
Blocker **geschlossen** sind. Bis zum Release offene, fertige Issues hielten dort alles dahinter
fest. Weil ein Milestone auch geschlossene Issues listet, geht dabei nichts verloren.

Findet die Geräteprüfung des Releases einen Fehler, wird er ein neues `bug`-Issue im selben
Milestone; das erledigte Issue bleibt geschlossen.

## PRs als Anfrage-Oberfläche: nein

_(Auf `ja` setzen, falls externe PRs als Feature-Wünsche behandelt werden sollen; `/triage`
liest dieses Flag.)_

## Pull Requests von außen

Eigene Arbeit geht ohne Pull Request direkt auf `main` (`CLAUDE.md`). Ein Pull Request von
außen ist ein Beitrag, kein Wunsch: Er bekommt `ready-for-human`, sobald er gebaut ist und auf
Prüfung und Merge wartet. Vor dem Merge gelten für ihn dieselben Tore wie für eigene Arbeit –
die GitHub-Action, `connectedAndroidTest` auf dem Emulator und die Code-Prüfung.

## Wenn ein Skill sagt „publish to the issue tracker"

Ein GitHub-Issue anlegen.

## Wenn ein Skill sagt „fetch the relevant ticket"

`gh issue view <nummer> --comments`

## Wayfinding

Genutzt von `/wayfinder`. Die **Karte** ist ein einzelnes Issue mit **Kind-Issues** als
Tickets.

- **Karte**: ein Issue mit Label `wayfinder:map`, das Notes / Decisions-so-far / Fog trägt.
- **Kind-Ticket**: als GitHub-Sub-Issue an die Karte gehängt (`gh api` auf den
  Sub-Issues-Endpunkt). Wo Sub-Issues nicht verfügbar sind: Eintrag in einer Task-Liste im
  Karten-Body plus `Part of #<karte>` oben im Kind-Body. Labels: `wayfinder:<typ>`
  (`research`/`prototype`/`grilling`/`task`).
- **Blockieren**: GitHub-eigene Issue-Dependencies via
  `gh api --method POST repos/<owner>/<repo>/issues/<kind>/dependencies/blocked_by -F issue_id=<db-id>`.
  `<db-id>` ist die numerische **Datenbank-Id** (`gh api repos/<owner>/<repo>/issues/<n> --jq .id`),
  nicht die `#nummer`. Fallback: Zeile `Blocked by: #<n>` oben im Kind-Body.
- **Frontier**: offene Kinder der Karte, ohne offenen Blocker und ohne Zuständigen; erste in
  Kartenreihenfolge gewinnt.
- **Beanspruchen**: `gh issue edit <n> --add-assignee @me`
- **Auflösen**: kommentieren, schließen, dann einen Verweis in „Decisions-so-far" der Karte
  ergänzen.
