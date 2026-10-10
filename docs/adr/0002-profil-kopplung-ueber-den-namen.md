# Profile hängen am Namen, nicht an einer ID

Ein Profil wird über seinen Namen identifiziert; die Historie verweist mit demselben Namen auf
das Kind, statt über eine eigens vergebene ID. Als die Benutzerverwaltung nachgerüstet wurde,
existierte bereits namensbasierte Historie auf den Geräten — eine ID hätte verlangt, diese
Altdaten zuzuordnen, ohne dass es dafür eine verlässliche Grundlage gab.

## Consequences

- Profilnamen sind eindeutig und wirken als Schlüssel: Umbenennen und Löschen müssen die
  Historie mitziehen, damit der Fortschritt am Kind hängen bleibt bzw. mit ihm verschwindet.
  Beides läuft deshalb als Transaktion.
- Beim Einspielen einer Sicherung werden Aufgaben zu einem Kind, für das kein Profil
  existiert, trotzdem übernommen. Wird später ein gleichnamiges Profil angelegt, findet es
  seinen alten Fortschritt wieder.
- Wer auf IDs umstellen will, braucht eine Migration, die Namen auf IDs abbildet — und muss
  das Sicherungsformat mitversionieren.
