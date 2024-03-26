package no.nav.tms.mikrofrontend.selector.regelmotor

import assert
import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.Produktfactory
import no.nav.tms.mikrofrontend.selector.collector.Produktfactory.IsInPeriodContentRule
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

class ProduktkortTest {
    @Test
    fun `avgjør om ett produktkort skal vises eller ikke`() {

        Produktfactory.getProduktkort(listOf(
            SafDokument(
                sakstemakode = "PEN",
                sistEndret = LocalDateTime.now()
            )
        ))
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "PEN"
                navn shouldBe "Pensjon"
                skalVises() shouldBe true
            }

        Produktfactory.getProduktkort(listOf(
            SafDokument(
                sakstemakode = "DAG",
                sistEndret = LocalDateTime.now()
            )
        ))
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "DAG"
                navn shouldBe "Dagpenger"
                skalVises() shouldBe true
            }
    }

    @ParameterizedTest
    @CsvSource(
        "DAG, Dagpenger, DAG",
        "FOR, Foreldrepenger, FOR",
        "HJE, Hjelpemidler, HJE",
        "KOM, Sosialhjelp, KOM",
        "PEN, Pensjon, PEN",
        "UFO, Uføretrygd, UFO",
        "SYK, Sykefravær, SYK",
        "SYM, Sykefravær, SYK"
    )
    fun `skal mappes til riktige koder og navn`(kode: String, forventetNavn: String, forventetKode: String) {
        Produktfactory.getProduktkort(
            listOf(SafDokument(kode, LocalDateTime.now()))
        ).first().assert {
            id shouldBe forventetKode
            navn shouldBe forventetNavn
        }
    }
    @Test
    fun `skal ikke legge til produktkort for ukjente verdier`() {
        Produktfactory.getProduktkort(
            listOf(SafDokument("ABC", LocalDateTime.now()))
        ).size shouldBe 0
    }

    @Test
    fun `periode etter siste dokument`() {
        IsInPeriodContentRule(3, LocalDateTime.now()).applyRule() shouldBe true
        IsInPeriodContentRule(3, LocalDateTime.now().minusWeeks(4)).applyRule() shouldBe false
    }
}