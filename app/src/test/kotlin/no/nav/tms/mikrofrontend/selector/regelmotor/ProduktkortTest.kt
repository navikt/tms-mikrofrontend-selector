package no.nav.tms.mikrofrontend.selector.regelmotor

import assert
import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.ProduktkortVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ProduktkortTest {
    @Test
    fun `avgjør om et dagpenge produktkort skal vises eller ikke`() {
        ProduktkortVerdier.resolveProduktkort(
            koder = listOf("DAG"), ident = "12345678910", microfrontends = null
        )
            .first()
            .skalVises("123", emptyList()) shouldBe true
    }
    @ParameterizedTest
    @CsvSource(
        "DAG, Dagpenger",
        "FOR, Foreldrepenger",
        "HJE, Hjelpemidler",
        "KOM, Sosialhjelp",
        "PEN, Pensjon",
        "UFO, Uføretrygd",
        "SYK, Sykefravær",
        "SYM, Sykefravær"
    )

    fun `skal mappes til riktige koder og navn`(kode: String, forventetNavn: String){
        ProduktkortVerdier.resolveProduktkort(
            koder = listOf(kode), ident = "12345678910", microfrontends = null
        ).first().assert {
            id shouldBe kode
            navn shouldBe forventetNavn
        }
    }

    @Test
    fun `skal ikke legge til produktkort for ukjente verdier`() {
        ProduktkortVerdier.resolveProduktkort(
            koder = listOf("ABC"), ident = "12345678910", microfrontends = null
        ).size shouldBe 0
    }
}