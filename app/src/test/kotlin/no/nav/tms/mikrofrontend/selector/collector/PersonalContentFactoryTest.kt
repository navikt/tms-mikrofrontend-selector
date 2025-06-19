package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.versions.Entry
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.HIGH
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.SUBSTANTIAL
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PersonalContentFactoryTest {
    val dokumentarkivUrlResolver = DokumentarkivUrlResolver(generellLenke = "https://www.nav.no", temaspesifikkeLenker = mapOf("DAG" to "https://www.nav.no/dokumentarkiv/dagpenger"))

    @Test
    fun `Skal være tom`() {
        testFactory().build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
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
            safResponse = SafResponse(listOf(Dokument("DAG", navn = "Dagpenger", dokumentarkivUrlResolver = dokumentarkivUrlResolver, sistEndret = LocalDateTime.now()))),
            digisosResponse = DigisosResponse(listOf(Dokument(kode = "KOM", navn = "Sosialhjelp", dokumentarkivUrlResolver = dokumentarkivUrlResolver, sistEndret = LocalDateTime.now()))),
        ).build(
            microfrontends = microfrontendMocck(level4Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 5),
            levelOfAssurance = HIGH,
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
            safResponse = SafResponse(emptyList(), listOf("Saf feilet fordi det gikk feil")),
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
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
            meldekortResponse = MeldekortResponse(errors = "Feil som skjedde")
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
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
            safResponse = SafResponse(
                dokumenter = listOf(Dokument("DAG", navn = "Dagpenger", dokumentarkivUrlResolver = dokumentarkivUrlResolver, sistEndret = LocalDateTime.now())),
                errors = emptyList()
            ),
            meldekortResponse = MeldekortResponse(JsonPathInterpreter.initPathInterpreter("{}")),
        ).build(
            microfrontends = microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 5,
                ids = listOf("aia-ny")

            ),
            levelOfAssurance = HIGH,
            discoveryManifest = DiscoveryManifest(mapOf("regefrontend" to Entry("https://micro.moc", "name", "ns", "https://app.fallback", true)))
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
            safResponse = SafResponse(
                listOf(Dokument("DAG", navn = "Dagpenger", dokumentarkivUrlResolver = dokumentarkivUrlResolver, sistEndret = LocalDateTime.now())),
                emptyList()
            )
        ).build(
            microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 5,
                level3Microfrontends = MicrofrontendsDefinition("id", "url", "appname", "namespace", "fallback", true) * 2,
            ),
            levelOfAssurance = SUBSTANTIAL,
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
    safResponse: SafResponse = SafResponse(emptyList(), emptyList()),
    meldekortResponse: MeldekortResponse = MeldekortResponse(JsonPathInterpreter.initPathInterpreter("{meldekort:0}")),
    pdlResponse: PdlResponse = PdlResponse(LocalDate.parse("1988-09-08"), 1988),
    digisosResponse: DigisosResponse = DigisosResponse(),
    levelOfAssurance: LevelOfAssurance = HIGH
) =
    PersonalContentFactory(
        safResponse = safResponse,
        meldekortResponse = meldekortResponse,
        pdlResponse = pdlResponse,
        digisosResponse = digisosResponse,
        levelOfAssurance = levelOfAssurance
    )

private fun microfrontendMocck(
    level3Microfrontends: List<MicrofrontendsDefinition> = emptyList(),
    level4Microfrontends: List<MicrofrontendsDefinition>? = null,
    ids: List<String> = listOf("mock")
) = mockk<Microfrontends> {
    every { offerStepup(HIGH) } returns false
    every { offerStepup(SUBSTANTIAL) } returns (level3Microfrontends != level4Microfrontends)
    every { getDefinitions(SUBSTANTIAL, any()) } returns level3Microfrontends
    every { getDefinitions(HIGH, any()) } returns (level4Microfrontends ?: level3Microfrontends)
    every { ids(any()) } returns ids
}
