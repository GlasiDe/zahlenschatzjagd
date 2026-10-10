# Triage-Labels

Die Skills sprechen von fünf kanonischen Triage-Rollen. Diese Datei bildet sie auf die
Label-Namen ab, die im Issue-Tracker dieses Repos tatsächlich benutzt werden.

| Rolle im Skill | Label in unserem Tracker | Bedeutung |
|---|---|---|
| `needs-triage` | `needs-triage` | Muss noch bewertet werden |
| `needs-info` | `needs-info` | Wartet auf Rückmeldung des Melders |
| `ready-for-agent` | `ready-for-agent` | Vollständig spezifiziert, ein Agent kann es allein bauen |
| `ready-for-human` | `ready-for-human` | Braucht einen Menschen: am Issue „muss ein Mensch bauen", am Pull Request „fertig gebaut, wartet auf Prüfung und Merge" |
| `wontfix` | `wontfix` | Wird nicht angegangen |

Nennt ein Skill eine Rolle („apply the AFK-ready triage label"), gilt der Label-Name aus der
rechten Spalte.

Die rechte Spalte lässt sich an ein abweichendes Vokabular anpassen. Hier ist sie
namensgleich.

## Zustandsübergänge

Ein Issue ohne Label geht zuerst nach `needs-triage`; von dort weiter nach `needs-info`,
`ready-for-agent`, `ready-for-human` oder `wontfix`. Aus `needs-info` geht es zurück nach
`needs-triage`, sobald der Melder antwortet.
