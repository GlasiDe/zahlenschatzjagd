# kotlinx.serialization statt des Android-eigenen org.json

Das Dateiformat der Sicherung wird mit **kotlinx.serialization** gelesen und geschrieben,
nicht mit dem in Android eingebauten `org.json`.

`org.json` ist Teil des Android-Frameworks und in reinen JVM-Unit-Tests nicht verfügbar (die
Stub-Implementierung wirft). Das Dateiformat ist aber genau der Teil, der ohne Gerät testbar
sein muss: Es entscheidet darüber, ob Kinder ihren Fortschritt behalten.

## Consequences

Die Serialisierung läuft vollständig in schnellen Unit-Tests, inklusive des
Golden-File-Tests aus [0005](./0005-persistierte-namen-entkoppelt.md). Dafür trägt die App
eine zusätzliche Bibliothek und ein Compiler-Plugin mit.
