package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.common.testutils.assert
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

class ProduktkortTest {
    val dokumentarkivUrlResolver = DokumentarkivUrlResolver(
        generellLenke = "https://www.nav.no",
        temaspesifikkeLenker = mapOf("DAG" to "https://www.nav.no/dokumentarkiv/dagpenger")
    )

    @Test
    fun `avgjør om ett produktkort skal vises eller ikke`() {

        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    kode = "PEN",
                    navn = "Pensjon",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.HIGH
        )
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "PEN"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    kode = "PEN",
                    navn = "Pensjon",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.SUBSTANTIAL
        )
            .also { it.size shouldBe 1 }

        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    kode = "PEN",
                    navn = "Pensjon",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.HIGH
        )
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "PEN"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    kode = "DAG",
                    navn = "Dagpenger",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.HIGH
        )
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "DAG"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    kode = "DAG",
                    navn = "Dagpenger",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.HIGH
        )
            .also { it.size shouldBe 1 }
            .first()
            .assert {
                id shouldBe "DAG"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    kode = "DAG",
                    navn = "Dagpenger",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.SUBSTANTIAL
        )
            .also { it.size shouldBe 0 }
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
            listOf(
                Dokument(
                    kode,
                    navn = "Pensjon",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.HIGH
        ).first().assert {
            id shouldBe forventetKode
        }
    }

    @Test
    fun `skal ikke legge til produktkort for ukjente verdier`() {
        ContentDefinition.getProduktkort(
            listOf(
                Dokument(
                    "ABC",
                    navn = "Pensjon",
                    dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.HIGH
        ).size shouldBe 0
    }
}
