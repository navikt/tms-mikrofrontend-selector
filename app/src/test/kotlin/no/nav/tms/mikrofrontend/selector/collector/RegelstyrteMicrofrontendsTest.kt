package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.safTestDokument
import no.nav.tms.mikrofrontend.selector.versions.Discovery
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import org.junit.jupiter.api.Test

class RegelstyrteMicrofrontendsTest {
    private val discoveryManifestWithPensjon = DiscoveryManifest(mapOf("pensjonskalkulator-microfrontend" to Discovery(
        "https://cdn.pensjon/manifest.json", "name", "pensjon", "http://pensjon/fallback", true
    )))

    @Test
    fun `pensjon skal vises hvis personen er over 40 år og ikke har sakstema pensjon`() {
        ContentDefinition.getAktueltContent(41, listOf("DAG".safTestDokument()), discoveryManifestWithPensjon,
            LevelOfAssurance.Substantial).run {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(41, listOf("DAG".safTestDokument()), discoveryManifestWithPensjon, LevelOfAssurance.High).run {
            size shouldBe 1
            first().id shouldBe "pensjonskalkulator-microfrontend"
            first().url shouldBe "https://cdn.pensjon/manifest.json"
        }
        ContentDefinition.getAktueltContent(
            alder = 41,
            listOf("PEN".safTestDokument()),
            discoveryManifestWithPensjon,
            LevelOfAssurance.High
        ).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("FOR".safTestDokument(), "DAG".safTestDokument()),
            discoveryManifestWithPensjon,
            levelOfAssurance = LevelOfAssurance.High
        ).size shouldBe 0
        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf("PEN".safTestDokument()),
            discoveryManifestWithPensjon,
            levelOfAssurance = LevelOfAssurance.Substantial
        ).size shouldBe 0

        ContentDefinition.getAktueltContent(
            alder = 39,
            listOf(),
            discoveryManifestWithPensjon,
            levelOfAssurance = LevelOfAssurance.High
        ).size shouldBe 0
    }
}
