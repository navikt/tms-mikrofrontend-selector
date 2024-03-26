package no.nav.tms.mikrofrontend.selector.collector

import assert
import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import org.junit.jupiter.api.Test

class RegelstyrteMicrofrontendsTest {
    private val manifestMapWithPensjon =
        mapOf("pensjonskalkulator-microfrontend" to "https://cdn.pensjon/manifest.json")
    @Test
    fun `pensjon skal vises hvis personen er over 40 år og ikke har sakstema pensjon`(){
        ContentDefinition.getAktueltContent(41, listOf("DAG"),manifestMapWithPensjon).assert {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe  "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(
            alder = 41,
            listOf("PEN"),
            manifestMapWithPensjon).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("FOR","DAG"),
            manifestMapWithPensjon).size shouldBe 0
    }
}