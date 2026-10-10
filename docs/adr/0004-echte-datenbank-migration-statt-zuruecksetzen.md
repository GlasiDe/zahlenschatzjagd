# Echte Datenbank-Migration statt destruktivem Zurücksetzen

Schema-Änderungen werden als echte Migration geschrieben, die vorhandene Daten überführt. Der
destruktive Fallback — Datenbank verwerfen und neu anlegen — greift nur noch für Schema-
Versionen, die keine Migration kennen.

> **Nachtrag:** Auch dieser Rest ist inzwischen fort. Fehlt ein Migrationsweg, wirft die App
> nichts mehr weg, sondern lässt die Daten liegen und geht in den **Schutzhalt** — siehe
> `docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`.

Anlass war die Einführung der Profile: Auf den Geräten lag bereits Historie, die nur einen
Namen kannte. Die Migration legt die Profil-Tabelle an und erzeugt aus den vorgefundenen Namen
Profile, sodass der Lernfortschritt das Update übersteht. Genau das war das Ziel der
Benutzerverwaltung — ein Zurücksetzen hätte sie sinnlos gemacht.

## Consequences

Jede weitere Schema-Änderung braucht eine Migration und einen Test, der sie gegen eine echte
Alt-Datenbank fährt. Das ist Aufwand pro Änderung, aber die Alternative kostet Kinder ihren
Fortschritt — und auf einem Gerät mit installiertem Release-Build gibt es keinen Weg, Daten
von außen zu retten, außer der Sicherung.
