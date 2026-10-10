# Dateidialog der Sicherung läuft ohne engen MIME-Filter

Beim Einspielen einer Sicherung wird der Dateidialog bewusst **ohne** Einschränkung auf
`application/json` geöffnet. Manche Dateimanager melden `.json` als
`application/octet-stream`; mit Filter wäre die eigene Sicherung dann gar nicht auswählbar —
ausgerechnet in der Situation, für die sie existiert. Stattdessen wird der Inhalt geprüft und
eine falsche Datei freundlich abgelehnt.

## Consequences

Der Dialog zeigt auch Dateien, die keine Sicherung sind. Das ist der Preis dafür, dass die
echte Sicherung immer auffindbar bleibt. Wer den Filter „nachrüstet", macht die Funktion auf
manchen Geräten unbenutzbar.

Die Dateiwahl läuft über das Storage Access Framework, deshalb braucht die App **keine**
Speicherberechtigung — sie kommt bis heute ohne jede Berechtigung aus.
