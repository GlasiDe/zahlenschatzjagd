# Kein Android-Auto-Backup

Die App schaltet Androids Auto-Backup ab (`allowBackup="false"`). Der Fortschritt verlässt das
Gerät nur noch, wenn ein Erwachsener im Elternbereich von Hand eine **Sicherung** anlegt.

Bis dahin standen im Manifest `allowBackup="true"` und daneben zwei unveränderte Vorlagen ohne
eine einzige Regel — die Voreinstellung also, und die bedeutet: alles. Damit wanderte die
gesamte Datenbank samt der Namen der Kinder ungefragt zu Google, in einer App, die sonst
bewusst **keine einzige Android-Berechtigung** verlangt. Das war niemandem aufgefallen, weil es
niemand entschieden hatte.

Zweiter Grund: Eine Wiederherstellung war der einzige realistische Weg, auf dem eine **neuere**
Datenbank je eine **ältere** App-Fassung treffen konnte. Ohne Auto-Backup verschwindet dieser
Fall fast von selbst (siehe `docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`).

## Considered Options

Eine abschaltbare Cloud-Sicherung als bewusste Wahl der Eltern wäre über einen eigenen
`BackupAgent` machbar gewesen. Dagegen sprach vor allem, dass sie sich nicht prüfen lässt: Ein
echter Test bräuchte `bmgr`, einen Backup-Transport und ein Google-Konto auf dem Gerät. Das wäre
ein Versprechen an Eltern, das kein Test dieses Projekts absichert.

## Consequences

Ein Netz, das bislang unbemerkt gespannt war, fällt weg: Geht ein Gerät verloren oder kaputt,
hilft nur noch die manuelle Sicherung. Wer das ändern will, baut die Cloud-Sicherung als
ausdrückliche, standardmäßig abgeschaltete Wahl im Elternbereich — und dann als eigenes
Vorhaben, nicht als Nebenwirkung einer Voreinstellung.
