package no.nav.tms.mikrofrontend.selector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.ProduktkortVerdier
import org.junit.jupiter.api.Test

class ProduktkortTest {
    @Test
    fun `avgjør om et dagpenge produktkort skal vises eller ikke`() {
        ProduktkortVerdier.resolveProduktkort(
            koder = listOf("DAG"), ident = "12345678910", microfrontends = null
        )
            .first()
            .skalVises("123", emptyList()) shouldBe true
    }
}