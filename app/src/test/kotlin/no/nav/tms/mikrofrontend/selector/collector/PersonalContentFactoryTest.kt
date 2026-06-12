package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.versions.Discovery
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PersonalContentFactoryTest {

    @Test
    fun `Skal være tom`() {
        testFactory().build(
            microfrontends = Microfrontends(),
            levelOfAssurance = LevelOfAssurance.High,
            discoveryManifest = DiscoveryManifest(emptyMap())
        ).run {
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha microfrontends og produktkort innlogingsnivå 4`() {
        testFactory(
            safResponse = ExternalResponse.ok(
                service = ExternalService.Saf,
                value = listOf(Tema("DAG", navn = "Dagpenger", url = "https://www.intern.dev.nav.no/dokumentarkiv/tema", sistEndret = LocalDateTime.now())),
            ),
            digisosResponse = ExternalResponse.ok(
                service = ExternalService.Digisos,
                value = listOf(Tema(kode = "KOM", navn = "Sosialhjelp", url = "https://www.nav.no", sistEndret = LocalDateTime.now()))

            ),
        ).build(
            microfrontends = microfrontendMocck(level4Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 5),
            levelOfAssurance = LevelOfAssurance.High,
            discoveryManifest = DiscoveryManifest(emptyMap())
        ).run {
            offerStepup shouldBe false
            produktkort shouldBe listOf("DAG", "KOM")
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 5
        }
    }

    @Test
    fun `skal ha returstatus 207 pga SAF`() {
        testFactory(
            safResponse = ExternalResponse.error(emptyList(), ExternalService.Saf, "Saf feilet fordi det gikk feil"),
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = LevelOfAssurance.High,
            discoveryManifest = DiscoveryManifest(emptyMap())
        ).run {
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            this.resolveStatus() shouldBe HttpStatusCode.MultiStatus
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha produkkort og aia-standard og 207 pga meldekort`() {
        testFactory(
            meldekortApiResponse = ExternalResponse.error(MeldekortStatus(false), ExternalService.MeldekortApi, "Feil som skjedde")
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = LevelOfAssurance.High,
            discoveryManifest = DiscoveryManifest(emptyMap())
        ).run {
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            this.resolveStatus() shouldBe HttpStatusCode.MultiStatus
            this.microfrontends shouldBe emptyList()
        }

    }

    @Test
    fun `skal ha produkkort, ny-aia, meldekort og microfrontends`() {
        testFactory(
            safResponse = ExternalResponse.ok(
                service = ExternalService.Saf,
                value = listOf(Tema("DAG", navn = "Dagpenger", url = "https://www.intern.dev.nav.no/dokumentarkiv/tema", sistEndret = LocalDateTime.now())),
            ),
            meldekortApiResponse = ExternalResponse.ok(
                service = ExternalService.MeldekortApi,
                value = MeldekortStatus(false)
            )
        ).build(
            microfrontends = microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 5,
                ids = listOf("aia-ny")

            ),
            levelOfAssurance = LevelOfAssurance.High,
            discoveryManifest = DiscoveryManifest(mapOf("regefrontend" to Discovery("https://micro.moc", "name", "ns", "https://app.fallback", true)))
        ).run {
            offerStepup shouldBe false
            produktkort shouldBe listOf("DAG")
            resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 5
        }

    }

    @Test
    fun `skal ha microfrontends men ikke produktkort innloggingsnivå 3`() {
        // er både aia og meldekort nivå 4? Hva med produktkort?
        testFactory(
            safResponse = ExternalResponse.ok(
                service = ExternalService.Saf,
                value = listOf(Tema("DAG", navn = "Dagpenger", url = "https://www.intern.dev.nav.no/dokumentarkiv/tema", sistEndret = LocalDateTime.now())),
            )
        ).build(
            microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 5,
                level3Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 2,
            ),
            levelOfAssurance = LevelOfAssurance.Substantial,
            discoveryManifest = DiscoveryManifest(emptyMap())
        ).run {
            offerStepup shouldBe true
            produktkort shouldBe listOf()
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 2
        }
    }
}

private operator fun MicrofrontendsDefinition.times(i: Int): List<MicrofrontendsDefinition> =
    (1..i).map {
        MicrofrontendsDefinition(id = "$id$it", url = "$url$it", appname = "$appname$it", namespace = "$namespace$it", fallback = "$fallback$it", ssr = ssr)
    }


private fun testFactory(
    safResponse: ExternalResponse<List<Tema>> = ExternalResponse.ok(ExternalService.Saf, emptyList()),
    meldekortApiResponse: ExternalResponse<MeldekortStatus> = ExternalResponse.ok(ExternalService.MeldekortApi, MeldekortStatus(false)),
    dpMeldekortResponse: ExternalResponse<MeldekortStatus> = ExternalResponse.ok(ExternalService.DpMeldekort, MeldekortStatus(false)),
    pdlResponse: ExternalResponse<Foedselsdato> = ExternalResponse.ok(ExternalService.Pdl, Foedselsdato(LocalDate .parse("1988-09-08"), 1988)),
    digisosResponse: ExternalResponse<List<Tema>> = ExternalResponse.ok(ExternalService.Digisos, emptyList()),
    levelOfAssurance: LevelOfAssurance = LevelOfAssurance.High
) =
    PersonalContentFactory(
        safResponse = safResponse,
        meldekortApiResponse = meldekortApiResponse,
        dpMeldekortResponse = dpMeldekortResponse,
        pdlResponse = pdlResponse,
        digisosResponse = digisosResponse,
        levelOfAssurance = levelOfAssurance
    )

private fun microfrontendMocck(
    level3Microfrontends: List<MicrofrontendsDefinition> = emptyList(),
    level4Microfrontends: List<MicrofrontendsDefinition>? = null,
    ids: List<String> = listOf("mock")
) = mockk<Microfrontends> {
    every { offerStepup(LevelOfAssurance.High) } returns false
    every { offerStepup(LevelOfAssurance.Substantial) } returns (level3Microfrontends != level4Microfrontends)
    every { getDefinitions(LevelOfAssurance.Substantial, any()) } returns level3Microfrontends
    every { getDefinitions(LevelOfAssurance.High, any()) } returns (level4Microfrontends ?: level3Microfrontends)
    every { ids(any()) } returns ids
}
