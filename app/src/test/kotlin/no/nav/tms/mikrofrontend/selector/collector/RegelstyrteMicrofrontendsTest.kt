package no.nav.tms.mikrofrontend.selector.collector

import assert
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RegelstyrteMicrofrontendsTest {
    private val manifestMapWithPensjon = mapOf(Pensjon.id to "https://cdn.test/pensjonmf.js")

    @Test
    fun `pensjon skal vises hvis personen er over 40 år og ikke har sakstema pensjon`() {
        Pensjon(
            alder = 41,
            sakstemaer = listOf("DAG", "FOR"),
            manifestMap = manifestMapWithPensjon
        ).skalVises() shouldBe true
        Pensjon(alder = 41, sakstemaer = emptyList(), manifestMap = emptyMap()).skalVises() shouldBe false
        Pensjon(alder = 39, sakstemaer = listOf("PEN"), manifestMap = manifestMapWithPensjon).skalVises() shouldBe false
        Pensjon(alder = 39, sakstemaer = emptyList(), manifestMap = manifestMapWithPensjon).skalVises() shouldBe false
        Pensjon(alder = 65, sakstemaer = listOf("PEN"), manifestMap = manifestMapWithPensjon).skalVises() shouldBe false
    }

    @Test
    fun `Skal få korrekt Aktuelt innhold`(){
        Akutelt.getAktueltContent(41, listOf("DAG"),manifestMapWithPensjon).assert {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe  "https://cdn.test/pensjonmf.js"
        }
        Akutelt.getAktueltContent(
            alder = 41,
            listOf("PEN"),
            manifestMapWithPensjon).size shouldBe 0
        Akutelt.getAktueltContent(
            alder = 39,
            listOf("PEN"),
            manifestMapWithPensjon).size shouldBe 0

        Akutelt.getAktueltContent(
            alder = 39,
            listOf(),
            manifestMapWithPensjon).size shouldBe 0
    }
}