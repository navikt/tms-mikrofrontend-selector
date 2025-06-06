package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import no.nav.tms.common.testutils.assert
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.safTestDokument
import no.nav.tms.mikrofrontend.selector.versions.Entry
import no.nav.tms.mikrofrontend.selector.versions.MicrofrontendManifest
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import org.junit.jupiter.api.Test

class RegelstyrteMicrofrontendsTest {
    private val manifestMapWithPensjon = MicrofrontendManifest(mapOf("pensjonskalkulator-microfrontend" to Entry("https://cdn.pensjon/manifest.json", "", "", true)))

    @Test
    fun `pensjon skal vises hvis personen er over 40 år og ikke har sakstema pensjon`() {
        ContentDefinition.getAktueltContent(41, listOf("DAG".safTestDokument()), manifestMapWithPensjon, LevelOfAssurance.SUBSTANTIAL).assert {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(41, listOf("DAG".safTestDokument()), manifestMapWithPensjon, LevelOfAssurance.HIGH).assert {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(
            alder = 41,
            listOf("PEN".safTestDokument()),
            manifestMapWithPensjon,
            LevelOfAssurance.HIGH
        ).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("FOR".safTestDokument(), "DAG".safTestDokument()),
            manifestMapWithPensjon,
            levelOfAssurance = LevelOfAssurance.HIGH
        ).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("PEN".safTestDokument()),
            manifestMapWithPensjon,
            levelOfAssurance = LevelOfAssurance.SUBSTANTIAL
        ).size shouldBe 0

        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf(),
            manifestMapWithPensjon,
            levelOfAssurance = LevelOfAssurance.HIGH
        ).size shouldBe 0
    }
}