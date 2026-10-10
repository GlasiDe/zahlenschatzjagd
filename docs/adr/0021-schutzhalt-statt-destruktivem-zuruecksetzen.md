# Schutzhalt statt destruktivem Zurücksetzen

Kennt die App keinen Weg vom vorgefundenen Fortschritt zu ihrer heutigen Form, wirft sie ihn
nicht mehr weg. Sie lässt ihn liegen und geht in den **Schutzhalt**: Statt der Startkarte
erscheint eine Meldung für Erwachsene, geübt wird bis zum nächsten Update nicht. Zusätzlich legt
sie vor jeder Umstellung eine **Rücklage** an.

Genauer, weil es beim Nachmessen auf dem Gerät auffiel: **Kein einziger Datensatz** wird
verändert, gelöscht oder angelegt. Byte-gleich bleibt die Datei aber nicht — noch bevor Room
die Version prüft, stellt es den Journal-Modus auf WAL um, und Androids `SQLiteDatabase` legt
ihre `android_metadata`-Tabelle an. Das ist ein weiteres Argument für die Rücklage: Sie
entsteht, bevor irgendjemand die Datei öffnet, und ist damit die einzige wirklich unberührte
Kopie.

`docs/adr/0004-echte-datenbank-migration-statt-zuruecksetzen.md` hatte diesen Notfall-Pfad
bewusst offen gelassen, damit die App nach einem Schema-Sprung überhaupt startet. Diese
Rangfolge war falsch herum: Sie entschied still zugunsten von „App läuft" und zu Lasten von
„Fortschritt bleibt". Ein Kind stand dann vor einer frischen App und fragte, wo seine
Schatztruhen sind — und zwar erst Tage später, wenn der Schaden nicht mehr zu beheben ist.

## Warum die App das Update nicht einfach ablehnen kann

Die naheliegende Antwort wäre, die neue Version gar nicht erst zu übernehmen. Das geht nicht:
Wenn die erste Zeile eigenen Codes läuft, ist die neue Fassung längst installiert und die alte
überschrieben; ein Rückweg ist unter Android versperrt. Was aber sehr wohl geht, ist die
Weigerung eine Ebene tiefer — die neue Fassung muss die vorhandenen Daten nicht anrühren. Genau
das ist der Schutzhalt. Das nächste Release findet den Fortschritt vollständig vor.

## Warum trotzdem eine Rücklage

Room führt den ganzen Umstellungslauf in einer Transaktion und macht bei einem Fehler
`ROLLBACK`; eine **fehlschlagende** Umstellung richtet also ohnehin keinen Schaden an. Es bleibt
genau eine Lücke: eine Umstellung, die **durchläuft** und dabei trotzdem Daten verliert, weil
sie falsch gedacht war. Dagegen hilft nur eine Kopie von vorher. Sie entsteht, sobald die
gespeicherte Form von der erwarteten abweicht, und bleibt bis zur nächsten Umstellung liegen —
nicht bis zum ersten erfolgreichen Öffnen, denn ein solcher Fehler fällt niemandem beim Öffnen
auf, sondern nächste Woche.

## Zum Zeitpunkt der Entscheidung

Jede je verteilte Fassung (1.1 bis 1.6) trug Schema-Version 3 oder höher, und ab 2 führt ein
lückenloser Weg nach oben. Auf keinem Gerät konnte das destruktive Zurücksetzen also je
zuschlagen. Es war keine offene Wunde, sondern eine Tretmine für das Release, bei dem eine
Migration vergessen wird. Deshalb fällt es ersatzlos weg — auch nicht eng begrenzt für
Alt-Versionen, für die es ohnehin keine Installation gibt.

Ebenfalls bestätigt: `fallbackToDestructiveMigration(dropAllTables = true)` setzt in Room 2.8.4
neben `requireMigration = false` auch `allowDestructiveMigrationOnDowngrade = true`. Eine
gespeicherte Form, die **neuer** ist als die erwartete, führte in denselben Verlust. Auch dieser
Weg endet nun im Schutzhalt, und weil der Erwachsene ihn selbst lösen kann — die neuere Fassung
wieder installieren —, nennt die Meldung diesen Fall gesondert.

## Consequences

Ein vergessener Migrationsweg macht die App unbenutzbar, statt sie zu leeren. Das ist gewollt,
verlangt aber, dass ein Release mit Schema-Änderung nie ungeprüft hinausgeht: Der Fehler ist
danach nur noch durch ein weiteres Release zu beheben, und auf fremden Geräten erfährt niemand
davon, außer jemand meldet es. Die Meldung nennt deshalb die drei Zahlen, die zum Nachbauen
reichen — gefundene Form, erwartete Form, App-Version —, und seit der Veröffentlichung auch, wo
gemeldet wird: in den Issues des Repos auf GitHub, am einfachsten mit einem Bildschirmfoto.

Die Rücklage kostet dauerhaft ein zweites Mal die Größe der Datenbank. Auslesen kann die App sie
vorerst nicht; das ist Absicht. Die Kopie lässt sich nicht nachträglich anlegen, das Lesegerät
dafür jederzeit nachliefern.

Die Entscheidung fällt in `ZahlenschatzjagdApp.onCreate`, also **bevor** der erste Bildschirm
gezeichnet wird. Das muss so sein: Die Rücklage hat nur einen Wert, wenn sie steht, bevor
irgendetwas die Datenbank öffnet, und ein fehlender Migrationsweg soll nicht irgendwann mitten
in einer Runde auffallen.

Der Preis dafür ist leicht zu übersehen: **Migrationen laufen damit auf dem Haupt-Thread.**
Vorher liefen sie im Hintergrund, weil der erste Zugriff aus einer Coroutine kam. Heute ist das
folgenlos — jede Migration dieses Projekts ist ein `CREATE TABLE` oder ein `ALTER TABLE` und
damit eine Sache von Millisekunden. Eine Migration, die **Daten umschreibt** statt nur Spalten
anzulegen, würde den Start dagegen einfrieren, und zwar auf einem Gerät mit viel Historie am
stärksten — also genau dort, wo am meisten zu verlieren ist. Wer eine solche Migration schreibt,
muss den Start vorher hinter einen sichtbaren Wartezustand legen. Das ist der Punkt, an dem
diese Entscheidung nachzuziehen ist, nicht die Migration.

Weil es immer nur **eine** Rücklage gibt, hat sie eine Reichweite von genau einer
Schema-Änderung: Die übernächste überschreibt sie mit dem dann vorliegenden — womöglich bereits
beschädigten — Stand. Eine Umstellung, die durchläuft und dabei Daten verliert, muss also vor
dem **nächsten** Release mit Schema-Änderung auffallen. Das ist der Preis dafür, nicht endlos
Kopien anzuhäufen; wer ihn nicht zahlen will, müsste die Rücklage vor jedem Überschreiben
aufheben.
