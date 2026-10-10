# Zahlenschatzjagd

Eine Lern-App, mit der Grundschulkinder selbstständig Mathematik üben. Sie richtet sich an
Kinder, die noch nicht sicher lesen: Der Weg durch die App führt über Bilder und eine
Vorlesestimme, nicht über Text.

Dieses Dokument legt die Sprache des Projekts fest — welches Wort für welchen Begriff gilt und
welche Wörter dafür vermieden werden. Es beschreibt **keine** Umsetzung: Warum etwas so gebaut
ist, steht in `docs/adr/`.

## Üben

**Runde**:
Ein zusammenhängender Durchgang aus einer vorher festgelegten Anzahl Aufgaben. Sie endet mit
einer Auswertung und Sternen. Was ein Kind darin übt und wie schwer es ist, wählt es vorher.
_Avoid_: Spiel, Session, Game, Durchlauf

**Aufgabe**:
Eine einzelne Frage innerhalb einer Runde, zusammen mit ihrer richtigen Lösung. Das Kind
beantwortet sie und bekommt sofort Rückmeldung.
_Avoid_: Frage, Rechnung, Item

**Übungsform**:
Eine Art zu üben — Grundrechenarten, Verdoppeln, Zahlenmauer, Verliebte Zahlen, Zahlen
vergleichen, Nachbarzahlen. Jede Übungsform hat eigene Aufgaben, eine eigene Darstellung und
eigene Abzeichen.
_Avoid_: Spiel, Modus, Rechenspiel, Zahlenspiel, Aufgabentyp

**Nachbarzahlen**:
Die Übungsform, in der drei aufeinanderfolgende Zahlen nebeneinander stehen: eine ist
vorgegeben, die beiden anderen ergänzt das Kind. Die Schrittweite ist immer 1. Nicht zu
verwechseln mit dem Fortsetzen einer Zahlenfolge mit anderer Schrittweite — das ist eine
andere Übungsform und heißt **Zahlenreihen fortsetzen**; gebaut ist sie nicht.
_Avoid_: Zahlenreihe, Zahlenfolge, Vorgänger-Nachfolger-Spiel

**Rechenart**:
Eine der vier Grundrechenarten: Plus, Minus, Mal, Geteilt. Die Rechenarten sind gemeinsam
**eine** Übungsform; innerhalb einer Runde lassen sich mehrere davon mischen.
_Avoid_: Operation, Typ, Kategorie, Rechenoperation

**Zahlenraum**:
Die Obergrenze der Zahlen, die in den Aufgaben vorkommen — bis 10, 20, 50 oder 100. Das Kind
wählt ihn selbst, sofern die Übungsform ihn offenlässt.
_Avoid_: Range, Level, Schwierigkeit, Zahlenbereich

**Schuleingangsphase**:
Die Klassen 1 und 2 als **eine** Einheit, die ein Kind in ein bis drei Jahren durchläuft. Der
NRW-Lehrplan nennt seine Kompetenzerwartungen nicht je Klasse, sondern zu zwei Zeitpunkten:
Ende der Schuleingangsphase und Ende Klasse 4. „Ende der Schuleingangsphase" heißt damit Ende
Klasse 2 — und nicht Klasse 1.
_Avoid_: Eingangsstufe, Anfangsunterricht, Klasse 1

**Schwierigkeitsstufe**:
Wie anspruchsvoll eine Übungsform gestellt wird — etwa leicht, mittel, schwer, ultra. Bewusst
getrennt vom Zahlenraum: Eine Stufe kann sich auch dadurch unterscheiden, wie viel Hilfe
angeboten wird oder wie die Antwort gegeben wird, nicht nur durch die Größe der Zahlen.
_Avoid_: Level, Grad, Zahlenraum, Modus

**Hilfe**:
Eine zusätzliche Veranschaulichung, die eine Aufgabe leichter macht, ohne sie zu verändern —
etwa Münzen, die zeigen, wie viel zu einer Zahl noch fehlt. Eine Übungsform mit Hilfe bleibt
dieselbe Übungsform; „mit Hilfe" und „ohne Hilfe" können aber zwei Schwierigkeitsstufen
sein, wie bei Verliebte Zahlen.
_Avoid_: Tipp, Hint, Unterstützung

## Kind und Fortschritt

**Profil**:
Ein Kind, das die App benutzt, mit seinem Namen und seinem Avatar. Mehrere Geschwister teilen
sich ein Gerät; jedes Profil hat seinen eigenen, getrennten Fortschritt.
_Avoid_: Nutzer, Benutzer, Account, User, Kind (als Fachbegriff)

**Avatar**:
Das Piratenbild, an dem ein Kind sein Profil wiedererkennt — die leseunabhängige Entsprechung
zum Namen.
_Avoid_: Bild, Icon, Figur

**Freigabe**:
Was ein Profil üben darf: eine Menge von Fortschrittskennungen und eine Menge von
Zahlenräumen. Erreichbar ist, was in beiden vorkommt; alles andere erscheint dem Kind gar
nicht erst. Die Zahlenräume betreffen nur Übungen, in denen das Kind den Zahlenraum selbst
wählt – eine Übung mit festem Zahlenraum ist allein über ihre Fortschrittskennung freigegeben.
_Avoid_: Sperre, Kindersicherung, Whitelist, Berechtigung, Erlaubnis

**Historie**:
Die dauerhafte Aufzeichnung aller beantworteten Aufgaben eines Profils. Sie ist die einzige
Quelle des Fortschritts: Abzeichen und Auswertungen werden aus ihr abgeleitet, nicht getrennt
mitgeführt.
_Avoid_: Log, Verlauf, Statistik, Ergebnisse

**Bildschirmzeit**:
Die Zeit, die ein Profil die App tatsächlich benutzt hat. Pausen ab zwei Minuten ohne
Eingabe zählen nicht mit, ebenso wenig die Zeit im Elternbereich.
_Avoid_: Sitzung, Session, Nutzungsdauer, Spielzeit, Verweildauer

**Zeitabschnitt**:
Die Einheit, in der Bildschirmzeit anfällt: ein Profil, ein Beginn, ein Ende. Ein Abschnitt
ist **unveränderlich** – er entsteht erst, wenn er zu Ende ist, und wird danach nie wieder
angefasst. Genau darauf beruht das Zusammenführen beim Einspielen einer Sicherung.
_Avoid_: Sitzung, Session, Intervall, Zeitspanne, Zeitraum

**Rundenabschluss**:
Das Ereignis, dass eine Runde **durchgezogen** wurde: ein Profil, ein Zeitpunkt. Er ist der
Punkt, den der Übungskalender an einem Tag setzt, und wie der Zeitabschnitt
**unveränderlich**. Ein Abbruch mitten in der Runde hinterlässt keinen.
_Avoid_: Rundenende, Streak-Eintrag, Übungstag, Session

**Fortschrittskennung**:
Die stabile Kennung, unter der eine Aufgabe in die Historie eingeht und nach der Fortschritt
getrennt gezählt wird. Sie unterscheidet feiner als die Übungsform: Zwei Schwierigkeitsstufen
derselben Übungsform können getrennte Kennungen haben, wenn sie unterschiedlich schwer sind.
Kennungen sind **unveränderlich** — sie ändern hieße, Kindern ihren Fortschritt zu nehmen.
_Avoid_: Kategorie, Key, Typ, Schlüssel

**Abzeichen**:
Das dauerhafte Sammelziel, dargestellt als Schatztruhe, die sich mit dem Üben füllt. Ein
Abzeichen gehört zu genau einer Fortschrittskennung.
_Avoid_: Badge, Medaille, Trophäe, Auszeichnung

**Stufe**:
Der erreichte Rang innerhalb eines Abzeichens — von Bronze bis Platin. Jede Stufe verlangt einen
bestimmten Gesamtertrag und legt einen weiteren Gegenstand in die Truhe.
_Avoid_: Level, Tier, Rang

**Ertrag**:
Wie viel eine beantwortete Aufgabe zu ihrem Abzeichen beiträgt. Bei den meisten Übungsformen 1
für richtig und 0 für falsch; eine Zahlenmauer bringt ganz richtig mehr und teilweise richtig
noch etwas. Ein Begriff des Projekts, nicht der App: Kinder und Eltern bekommen ihn nie zu
sehen — die Truhe füllt sich einfach.
_Avoid_: Punkte, Score, Gewicht, Wert

**Stern**:
Rückmeldung am Ende einer Runde, null bis drei, je nach Trefferquote. Sterne gelten nur für die
gerade gespielte Runde und werden nicht gesammelt — im Unterschied zum Abzeichen.
_Avoid_: Punkte, Score, Bewertung

**Sicherung**:
Eine Datei, die den gesamten Fortschritt aller Profile enthält — Historie, Bildschirmzeit und
Rundenabschlüsse —, gedacht für Gerätewechsel und Neuinstallation. Ein Erwachsener legt sie im
Elternbereich von Hand an. Beim Einspielen wird **zusammengeführt**, nicht ersetzt: Vorhandener
Fortschritt bleibt, Bekanntes wird nicht doppelt angelegt.
_Avoid_: Backup, Export, Datensicherung

**Rücklage**:
Die Kopie des gespeicherten Fortschritts, die die App **selbst** anlegt, kurz bevor sie ihn auf
eine neue Form bringt. Anders als die Sicherung verlangt sie kein Zutun, verlässt das Gerät
nicht und ist für Menschen nicht lesbar; sie liegt für den Fall bereit, dass die Umstellung
zwar durchläuft, dabei aber etwas verlorengeht. Es gibt immer höchstens eine — die nächste
Umstellung ersetzt sie.
_Avoid_: Sicherung, Backup, Sicherungskopie, Snapshot

**Schutzhalt**:
Der Zustand, in dem die App den vorgefundenen Fortschritt **nicht anfasst**, weil sie ihn nicht
sicher weiterverwenden kann. Statt der Startkarte erscheint eine Meldung für Erwachsene; geübt
wird bis zum nächsten Update nicht. Der Name sagt beide Hälften: Die App hält an, **weil** sie
schützt. Sie ist dabei nicht kaputt — sie weigert sich.
_Avoid_: Fehlerzustand, Absturz, Wartungsmodus, Notfallmodus

## Oberfläche

**Startkarte**:
Der Einstieg in die App, gezeichnet als Schatzkarte. Von hier führt jeder Weg weiter; sie ist
der einzige Bildschirm, zu dem alle anderen zurückführen.
_Avoid_: Startseite, Hauptmenü, Home

**Insel**:
Ein Bereich der App, als Insel auf der Startkarte dargestellt und mit einem großen Symbol
angetippt. Neue Übungswelten kommen als weitere Insel dazu.
_Avoid_: Kachel, Button, Bereich, Menüpunkt

**Maskottchen**:
Ella, die Figur, die das Kind begrüßt und ihm vorliest. Sie ist bewusst ein Mädchen, das Mathe
kann, und bleibt dieselbe, auch wenn sich der Name der App ändert.
_Avoid_: Charakter, Guide, Assistent

**Vorlesefunktion**:
Die gesprochene Ausgabe von Beschriftungen, damit Kinder ohne sichere Lesefähigkeit sich allein
zurechtfinden. Startkarte und Menüs begrüßen bei jedem **Besuch** — wenn das Kind von einem
anderen Bildschirm kommt, nicht aber, wenn es nur das Gerät dreht. Fehlt die Sprachausgabe auf dem
Gerät, bleibt die App still — sie meldet keinen Fehler. Ein Schalter im Elternbereich schaltet sie
ab; er gilt für das Gerät, nicht für ein Kind.
_Avoid_: Sprachausgabe, TTS, Audio

**Ton**:
Die kurzen Klänge, die eine Antwort und das Ende einer Runde begleiten — richtig, falsch,
geschafft. Ein Schalter im Elternbereich schaltet sie ab; er gilt wie der der Vorlesefunktion für
das Gerät und nicht für ein Kind.
_Avoid_: Sound, Audio, Geräusch, Effekt

**Profilverwaltung**:
Der Bildschirm hinter dem Profil-Abzeichen der Startkarte, auf dem alle Profile als Karten
liegen. **Wechseln** ist der Alltagsvorgang und bleibt frei; anlegen, umbenennen, Avatar ändern
und löschen liegen hinter Elternfrage bzw. PIN. Ein Schalter im Elternbereich zieht auch das
Wechseln dahinter.
_Avoid_: Benutzerverwaltung, Nutzerverwaltung, Profilauswahl

**Elternbereich**:
Der Teil der App, der sich an Erwachsene richtet: eine Übersicht mit sechs Einträgen — Zugang,
Übungsstand, Übungskalender, Freigabe, Ton & Vorlesen sowie die Sicherung des Fortschritts —,
jeder auf einem eigenen Bildschirm. Der **Zugang** steht an erster Stelle und regelt die Tür
selbst: die optionale PIN und die Frage, ob auch der Profilwechsel durch sie muss.
_Avoid_: Einstellungen, Admin-Bereich, Settings

**Übungsstand**:
Die Sicht für Erwachsene darauf, **was** ein Kind zuletzt geübt hat und **wie sicher** es
sitzt. Anders als die Historie ist er ein Ausschnitt: nur die jüngsten Aufgaben je
Fortschrittskennung.
_Avoid_: Statistik, Lernstand, Auswertung, Fortschrittsübersicht

**Übungskalender**:
Die Sicht für Erwachsene darauf, **wann** und **wie lange** ein Kind geübt hat. Ein Tag zählt
als geübt, sobald eine Runde abgeschlossen wurde.
_Avoid_: Streak, Serie, Verlauf, Statistik

**Tor**:
Die Prüfung, die vor Elternbereich und Profilverwaltung steht — die Elternfrage, oder die PIN,
sobald eine gesetzt ist. Ein Wort für beide, weil überall dieselbe Prüfung gilt und sich nur
unterscheidet, was dahinter liegt.
_Avoid_: Gate, Sperre, Schranke, Kindersicherung

**Elternfrage**:
Die kleine Rechenaufgabe vor dem Elternbereich und vor der Profilverwaltung, die
Grundschulkinder noch nicht lösen. Sie hält Kinder beiläufig fern, ohne ein Passwort zu
verlangen. Sie gilt, solange keine PIN gesetzt ist.
_Avoid_: Adult-Gate, Kindersicherung, Sperre

**PIN**:
Die vierstellige Zahl, die Elternbereich und Profilverwaltung zusätzlich sichert — optional und
gerätegebunden.
Ist eine gesetzt, tritt sie an die Stelle der Elternfrage. Vergeben lässt sie sich nur von
innen, also hinter Elternfrage oder alter PIN. Sie wird nie angezeigt, sondern liegt allein als
Prüfsumme auf dem Gerät; wer sie vergisst, vergibt über die Sicherheitsfrage eine neue.
_Avoid_: Passwort, Code, Kennwort, Kindersicherung

**Sicherheitsfrage**:
Die frei formulierte Frage samt Antwort, die beim Vergeben einer PIN angelegt wird und den
Rückweg öffnet, wenn die PIN vergessen ist. Frei formuliert, weil feste Fragenlisten im
Haushalt wertlos sind: Haustier und Lieblingsessen kennt das Kind besser als die Eltern. Sie
überdauert einen bloßen Wechsel der PIN; ändern muss man sie ausdrücklich.
_Avoid_: Wiederherstellungsfrage, Geheimfrage, Passwort-Hinweis
