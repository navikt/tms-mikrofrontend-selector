package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.kotest.matchers.shouldBe
import nav.no.tms.common.testutils.assert

import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

class ProduktkortTest {
    @Test
    fun `avgjør om ett produktkort skal vises eller ikke`() {

        ContentDefinition.getProduktkort(listOf(
            SafDokument(
                sakstemakode = "PEN",
                sistEndret = LocalDateTime.now()
            )
        ))
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "PEN"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(listOf(
            SafDokument(
                sakstemakode = "DAG",
                sistEndret = LocalDateTime.now()
            )
        ))
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "DAG"
                skalVises() shouldBe true
            }
    }

    @ParameterizedTest
    @CsvSource(
        "DAG, DAG",
        "FOR, FOR",
        "HJE, HJE",
        "KOM, KOM",
        "PEN, PEN",
        "UFO, UFO",
        "SYK, SYK",
        "SYM, SYK"
    )
    fun `skal mappes til riktige koder og navn`(kode: String, forventetKode: String) {
        ContentDefinition.getProduktkort(
            listOf(SafDokument(kode, LocalDateTime.now()))
        ).first().assert {
            id shouldBe forventetKode
        }
    }
    @Test
    fun `skal ikke legge til produktkort for ukjente verdier`() {
        ContentDefinition.getProduktkort(
            listOf(SafDokument("ABC", LocalDateTime.now()))
        ).size shouldBe 0
    }
}