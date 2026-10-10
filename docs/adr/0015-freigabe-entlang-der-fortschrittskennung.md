# Freigabe entlang der Fortschrittskennung, nicht der Übungsform

Was ein Profil üben darf, wird als **zwei Mengen** gespeichert: freigegebene
Fortschrittskennungen und freigegebene Zahlenräume. Erreichbar ist, was in beiden vorkommt —
„Plus bis 10" entsteht also aus `{PLUS}` und `{10}`, nicht aus einem eigenen Eintrag je
Kombination.

Die Fortschrittskennung ist in diesem Projekt bereits die Einheit, nach der Fortschritt
getrennt gezählt wird (`CONTEXT.md`). Dieselbe Einheit für die Freigabe zu nehmen, spart eine
zweite Taxonomie neben einer bestehenden: Der Katalog liegt als `BADGE_CATEGORIES` fertig
vor, die Gruppierung für die Anzeige als `BADGE_TABS` ebenfalls.

## Considered Options

- **Achse „Übungsform" statt Kennung** (5 statt 12 Einträge). Zu grob: Verdoppeln (leicht)
  und Verdoppeln (schwer) hingen an einem Schalter, ebenso alle vier Zahlenmauer-Stufen. Es
  hätte außerdem erklärt werden müssen, wann Stufen zu ihrer Übungsform gehören und wann
  nicht.
- **Volles Kreuzprodukt aus Kennung und Zahlenraum** (27 Schalter, deckungsgleich mit den
  Schatztruhen). Erlaubt Fälle wie „Plus bis 100, aber Vergleichen nur bis 20" — in einer
  Familie stellt die niemand ein, der Preis wäre ein Bildschirm, den man nicht öffnen will.
- **Achsen „Rechenart × Zahlenraum"** (der erste Entwurf). Kann die Absicht „nur Addition bis
  10" nicht ausdrücken: Verdoppeln, Verliebte Zahlen und Zahlen vergleichen blieben sichtbar,
  weil sie keine Rechenart haben.

## Consequences

- ~~**Der Zahlenraum wirkt auch dort, wo das Kind ihn nicht wählt.**~~ Umgekehrt durch
  `docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md`. Ursprünglich: Zahlenmauer (mittel) hat
  fest den Zahlenraum 50 und ist nur erreichbar, wenn Kennung **und** 50 freigegeben sind.
  Die Aussage „Zahlen über 50 sind für dieses Kind zu groß" gilt unabhängig davon, über
  welche Übungsform sie hereinkommen.
- **Nicht Freigegebenes ist unsichtbar, nicht gedämpft.** Was keine erlaubte Option mehr hat,
  verschwindet eine Ebene höher: leere Kachel, dann leere Insel. Das vorhandene
  `Island.enabled` bedeutet weiterhin „gibt es noch nicht" und wird dafür **nicht**
  mitbenutzt — sonst sähen „kommt bald" und „ist dir verboten" identisch aus.
- **Es gibt keine Untergrenze.** Alles abzuwählen ist ein erreichbarer Zustand; die Startkarte
  zeigt dann nur noch Abzeichen und Für Eltern. Bewusst nicht abgefangen.
- **Abzeichen bleiben vollständig sichtbar**, auch für Nicht-Freigegebenes. Erreichtes wird
  nie weggenommen, und leere Truhen für Zukünftiges sind ohnehin der Normalfall.
- **Neue Kennungen sind für bestehende Profile freigegeben.** Kommt später eine Übungsform
  dazu, darf sie nicht stillschweigend hinter einer Freigabe verschwinden, die vor ihrer
  Existenz gesetzt wurde.
- **Kein Enddatum.** Eine Freigabe gilt, bis ein Erwachsener sie ändert. Eine befristete
  Verengung („diese Woche nur Minus") war vorgesehen und wurde verworfen: Sie hätte einen
  Vorgängerzustand mitführen müssen und einen Stichtag, an dem sich die App unaufgefordert
  ändert.
