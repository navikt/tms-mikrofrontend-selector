package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.Tema
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

class ProduktkortTest {
    val dokumentarkivUrlResolver = DokumentarkivUrlResolver(
        generellLenke = "https://www.nav.no",
        temaspesifikkeLenker = mapOf("DAG" to "https://www.intern.dev.nav.no/dokumentarkiv/tema")
    )

    @Test
    fun `avgjør om ett produktkort skal vises eller ikke`() {

        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    kode = "PEN",
                    navn = "Pensjon",
                    url = "https://www.nav.no",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.High
        )
            .also { it.size shouldBe 1 }
            .first()
            .run {
                id shouldBe "PEN"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    kode = "PEN",
                    navn = "Pensjon",
                    url = "https://www.nav.no",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.Substantial
        )
            .also { it.size shouldBe 1 }

        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    kode = "PEN",
                    navn = "Pensjon",
                    url = "https://www.nav.no",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.High
        )
            .also { it.size shouldBe 1 }
            .first()
            .run {
                id shouldBe "PEN"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    kode = "DAG",
                    navn = "Dagpenger",
                    url = "https://www.intern.dev.nav.no/dokumentarkiv/tema",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.High
        )
            .also { it.size shouldBe 1 }
            .first()
            .run {
                id shouldBe "DAG"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    kode = "DAG",
                    navn = "Dagpenger",
                    url = "https://www.intern.dev.nav.no/dokumentarkiv/tema",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.High
        )
            .also { it.size shouldBe 1 }
            .first()
            .run {
                id shouldBe "DAG"
                skalVises() shouldBe true
            }

        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    kode = "DAG",
                    navn = "Dagpenger",
                    url = "https://www.intern.dev.nav.no/dokumentarkiv/tema",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.Substantial
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
                Tema(
                    kode,
                    navn = "Pensjon",
                    url = "https://www.nav.no",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.High
        ).first().run {
            id shouldBe forventetKode
        }
    }

    @Test
    fun `skal ikke legge til produktkort for ukjente verdier`() {
        ContentDefinition.getProduktkort(
            listOf(
                Tema(
                    "ABC",
                    navn = "Pensjon",
                    url = "https://www.nav.no",
                    sistEndret = LocalDateTime.now()
                )
            ), LevelOfAssurance.High
        ).size shouldBe 0
    }
}
