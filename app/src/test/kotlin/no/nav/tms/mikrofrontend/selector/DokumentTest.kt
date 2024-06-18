package no.nav.tms.mikrofrontend.selector

import io.kotest.matchers.shouldBe
import no.nav.tms.common.testutils.assert
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.mikrofrontend.selector.collector.Dokument.Companion.getLatest
import org.junit.jupiter.api.Test
import java.time.LocalDateTime


class DokumentTest {
    val dokumentarkivUrlResolver = DokumentarkivUrlResolver(temaspesifikkeLenker = mapOf("KOM" to "abc"), generellLenke = "Karson")

    @Test
    fun `skal hente de to siste dokumentene sortert på dato`() {
        listOf<Dokument>(
            testDokument("KOM", LocalDateTime.now().minusDays(5)),
            testDokument("DAG", LocalDateTime.now()),
            testDokument("SYK", LocalDateTime.now().minusDays(2))
        ).getLatest().assert {
            first().kode shouldBe "DAG"
            last().kode shouldBe "SYK"
        }

    }

    fun `skal hente de to siste dokumentene sortert på dato i kombinerte lister`() {
        val listOne = listOf<Dokument>(
            testDokument("KOM", LocalDateTime.now().minusDays(5)),
            testDokument("DAG", LocalDateTime.now()),
            testDokument("SYK", LocalDateTime.now().minusDays(2))
        )
        val listTwo = listOf<Dokument>(
            testDokument("KOM", LocalDateTime.now().minusDays(5)),
            testDokument("DAG", LocalDateTime.now()),
            testDokument("SYK", LocalDateTime.now().minusDays(2))
        )

        (listOne + listTwo).getLatest().assert {
            first().kode shouldBe "DAG"
            last().kode shouldBe "DAG"
        }

    }

    private fun testDokument(kode: String, sistEndret: LocalDateTime) =
        Dokument(
            kode = kode,
            dokumentarkivUrlResolver = dokumentarkivUrlResolver,
            sistEndret = sistEndret,
            navn = "Kode"
        )
    }