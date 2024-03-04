package no.nav.tms.mikrofrontend.selector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.produktkortene
import org.junit.jupiter.api.Test

class ProduktkortTest {
    @Test
    fun `avgjør om et dagpenge produktkort skal vises eller ikke`() {
        produktkortene["DAG"]!!.skalVises() shouldBe true
    }
}