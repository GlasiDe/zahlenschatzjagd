package io.github.glaside.zahlenschatzjagd.data

/**
 * Baut den Kopf einer SQLite-Datei nach, wie ihn [leseSchemaVersion] liest: 16 Byte Kennung,
 * dann Füllung, und an Position 60 die Schema-Version als 4 Byte, höchstwertiges Byte zuerst.
 *
 * **An einer Stelle**, weil zwei Testklassen ihn brauchen und das abschließende Null-Byte der
 * Kennung im Quelltext unsichtbar ist: Stünde dort versehentlich ein Leerzeichen, wäre die
 * gebaute Datei keine SQLite-Datei mehr, und die Tests prüften unbemerkt den falschen Zweig.
 */
fun sqliteKopf(version: Int): ByteArray {
    val kopf = ByteArray(100)
    // Kennung plus abschliessendes Null-Byte - ausdruecklich angehaengt statt als Escape
    // im Text, damit im Quelltext kein unsichtbares Zeichen steht.
    ("SQLite format 3".toByteArray(Charsets.US_ASCII) + 0.toByte()).copyInto(kopf)
    kopf[60] = (version ushr 24).toByte()
    kopf[61] = (version ushr 16).toByte()
    kopf[62] = (version ushr 8).toByte()
    kopf[63] = version.toByte()
    return kopf
}
