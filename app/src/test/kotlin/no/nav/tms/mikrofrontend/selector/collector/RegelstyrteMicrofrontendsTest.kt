package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.safTestDokument
import no.nav.tms.mikrofrontend.selector.versions.Entry
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import org.junit.jupiter.api.Test

class RegelstyrteMicrofrontendsTest {
    private val discoveryManifestWithPensjon = DiscoveryManifest(mapOf("pensjonskalkulator-microfrontend" to Entry(
        "https://cdn.pensjon/manifest.json", "name", "pensjon", "http://pensjon/fallback", true
    )))

    @Test
    fun `pensjon skal vises hvis personen er over 40 år og ikke har sakstema pensjon`() {
        ContentDefinition.getAktueltContent(41, listOf("DAG".safTestDokument()), discoveryManifestWithPensjon, LevelOfAssurance.SUBSTANTIAL).run {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(41, listOf("DAG".safTestDokument()), discoveryManifestWithPensjon, LevelOfAssurance.HIGH).run {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(
            alder = 41,
            listOf("PEN".safTestDokument()),
            discoveryManifestWithPensjon,
            LevelOfAssurance.HIGH
        ).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("FOR".safTestDokument(), "DAG".safTestDokument()),
            discoveryManifestWithPensjon,
            levelOfAssurance = LevelOfAssurance.HIGH
        ).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("PEN".safTestDokument()),
            discoveryManifestWithPensjon,
            levelOfAssurance = LevelOfAssurance.SUBSTANTIAL
        ).size shouldBe 0

        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf(),
            discoveryManifestWithPensjon,
            levelOfAssurance = LevelOfAssurance.HIGH
        ).size shouldBe 0
    }
}
