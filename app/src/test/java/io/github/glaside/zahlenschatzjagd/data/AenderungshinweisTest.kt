package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Welche Änderungshinweise beim Start fällig sind (#8). Die Hinweisliste enthält wie in der App nur
 * markierte Versionen – hier 18, 20 und 21; 19 ist eine unmarkierte Wartungsversion.
 */
class AenderungshinweisTest {

    private val v18 = Aenderungshinweis(18, "2.0.2", "Achtzehn")
    private val v20 = Aenderungshinweis(20, "2.1.0", "Zwanzig")
    private val v21 = Aenderungshinweis(21, "2.2.0", "Einundzwanzig")
    private val markiert = listOf(v20, v18, v21)

    private fun faellig(zuletztGezeigt: Int?, aktuell: Int, profileVorhanden: Boolean = true) =
        faelligeAenderungshinweise(zuletztGezeigt, aktuell, profileVorhanden, markiert)

    @Test
    fun nachDemUpdateErscheintDerHinweisDerNeuenVersion() {
        assertEquals(listOf(v20), faellig(zuletztGezeigt = 19, aktuell = 20))
    }

    @Test
    fun uebersprungeneVersionenKommenMit_derNeuesteOben() {
        assertEquals(listOf(v21, v20, v18), faellig(zuletztGezeigt = 17, aktuell = 21))
    }

    @Test
    fun eineUnmarkierteVersionZeigtNichts() {
        assertTrue(faellig(zuletztGezeigt = 18, aktuell = 19).isEmpty())
    }

    @Test
    fun wasSchonGezeigtIstKommtNichtNochEinmal() {
        assertTrue(faellig(zuletztGezeigt = 20, aktuell = 20).isEmpty())
    }

    @Test
    fun hinweiseKuenftigerVersionenBleibenAussenVor() {
        assertEquals(listOf(v20), faellig(zuletztGezeigt = 19, aktuell = 20))
        assertTrue(faellig(zuletztGezeigt = 19, aktuell = 19).isEmpty())
    }

    @Test
    fun eineNeuinstallationZeigtNichts() {
        assertTrue(faellig(zuletztGezeigt = null, aktuell = 21, profileVorhanden = false).isEmpty())
    }

    /**
     * Die erste Version, die sich etwas merkt, findet auch bei bestehenden Familien keinen Stand
     * vor. Ohne den Blick auf die Profile bliebe gerade dieses Update stumm.
     */
    @Test
    fun ohneStandAberMitProfilenZeigtSichNurDieAktuelleVersion() {
        assertEquals(listOf(v21), faellig(zuletztGezeigt = null, aktuell = 21))
        assertTrue(faellig(zuletztGezeigt = null, aktuell = 19).isEmpty())
    }
}
