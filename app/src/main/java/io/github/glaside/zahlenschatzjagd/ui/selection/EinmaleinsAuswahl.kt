package io.github.glaside.zahlenschatzjagd.ui.selection

import io.github.glaside.zahlenschatzjagd.EINMALEINS_REIHEN
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.einmaleinsKennung

/**
 * Was der Einmaleins-Auswahl-Bildschirm anbietet – reine Logik ohne Compose, damit sich das Filtern
 * nach der Freigabe ohne Gerät prüfen lässt (wie [sichtbareRechenspiele]).
 */

/**
 * Die Stufen, in denen noch mindestens eine Reihe freigegeben ist, die leichtere zuerst. Die erste
 * davon ist vorgewählt; steht hier nur eine, gibt es keine Stufenwahl.
 */
fun einmaleinsStufen(freigabe: Freigabe): List<EinmaleinsSchwierigkeitsstufe> =
    EinmaleinsSchwierigkeitsstufe.entries.filter { einmaleinsReihen(freigabe, it).isNotEmpty() }

/** Die Reihen, die in dieser Stufe freigegeben sind – nur sie stehen im Raster. */
fun einmaleinsReihen(freigabe: Freigabe, stufe: EinmaleinsSchwierigkeitsstufe): List<Int> =
    EINMALEINS_REIHEN.filter { freigabe.erlaubt(einmaleinsKennung(it, stufe)) }

/**
 * Welche Haken nach einem Wechsel der Stufe stehen bleiben: die Reihen, die auch dort freigegeben
 * sind. Neu angehakt wird nichts – vorgewählt ist keine Reihe, auch nicht die einzige.
 */
fun reihenNachStufenwechsel(angehakt: Set<Int>, freigegeben: List<Int>): Set<Int> =
    angehakt.intersect(freigegeben.toSet())
