# Manuelle Dependency Injection statt Hilt

Die Application-Klasse erzeugt beim Start einen Container, der die Repositories
bereitstellt; Bildschirme und ViewModels holen sich ihre Abhängigkeiten daraus. Kein Hilt,
kein Koin.

Bei dieser Projektgröße — eine Handvoll Repositories, eine Datenbank, ein DataStore — kostet
ein DI-Framework mehr an Annotationen, Code-Generierung und Build-Zeit, als es an
Verdrahtung spart. Der Container ist eine Datei, die man in einer Minute liest.

## Consequences

Wächst die Zahl der Abhängigkeiten deutlich oder kommen mehrere Module dazu, ist der
Wechsel zu Hilt der naheliegende nächste Schritt. Bis dahin ist „warum kein Hilt?" mit
diesem ADR beantwortet.
