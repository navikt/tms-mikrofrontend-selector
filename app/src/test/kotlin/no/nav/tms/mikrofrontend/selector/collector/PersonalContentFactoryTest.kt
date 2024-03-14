package no.nav.tms.mikrofrontend.selector.collector

import assert
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import org.junit.jupiter.api.Test

class PersonalContentFactoryTest {

    @Test
    fun `Skal være tom`() {
        testFactory().build(
            microfrontends = Microfrontends(),
            innloggetnivå = 4,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandardWrapper shouldBe false
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha microfrontends og produktkort innlogingsnivå 4`() {
        testFactory(
            safResponse = SafResponse(listOf("DAG"), emptyList())
        ).build(
            microfrontends = microfrontendMocck(level4Microfrontends = MicrofrontendsDefinition("id", "url") * 5),
            innloggetnivå = 4,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe listOf("DAG")
            aiaStandardWrapper shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 5
        }
    }

    @Test
    fun `skal ha aia-standard, og returstatus 207 pga SAF`() {
        testFactory(
            safResponse = SafResponse(emptyList(), listOf("Saf feilet fordi det gikk feil")),
            arbeidsøkerResponse = ArbeidsøkerResponse(
                erArbeidssoker = true,
                erStandard = true,
                errors = emptyList()
            )
        ).build(
            microfrontends = Microfrontends(),
            innloggetnivå = 4,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandardWrapper shouldBe true
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.MultiStatus
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha oppfolgingcontent`() {
        testFactory(
            oppfolgingResponse = OppfolgingResponse(true, emptyList())

        ).build(
            microfrontends = Microfrontends(),
            innloggetnivå = 4,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe true
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandardWrapper shouldBe false
            oppfolgingContent shouldBe true
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha produkkort og aia standard og 207 pga oppfolging`() {
        testFactory(
            arbeidsøkerResponse = ArbeidsøkerResponse(
                erArbeidssoker = true,
                erStandard = true,
                errors = emptyList()
            ),
            oppfolgingResponse = OppfolgingResponse(null, listOf("Feil fra oppføging"))

        ).build(
            microfrontends = Microfrontends(),
            innloggetnivå = 4,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandardWrapper shouldBe true
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.MultiStatus
            this.microfrontends shouldBe emptyList()
        }

    }

    @Test
    fun `skal ha produkkort, aiastandard, oppfolging, meldekort og microfrontends`() {
        //TODO
        testFactory(
            arbeidsøkerResponse = ArbeidsøkerResponse(true, true, emptyList()),
            safResponse = SafResponse(listOf("DAG"), emptyList()),
            meldekortResponse = MeldekortResponse(todo = true, errors = emptyList()),
            oppfolgingResponse = OppfolgingResponse(underOppfolging = true, errors = emptyList()),
        ).build(
            microfrontends = microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url") * 5
            ),
            innloggetnivå = 4,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe true
            offerStepup shouldBe false
            produktkort shouldBe listOf("DAG")
            aiaStandardWrapper shouldBe true
            oppfolgingContent shouldBe true
            resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 5
        }

    }

    @Test
    fun `skal ha microfrontends og produktkort innloggingsnivå 3`() {
        //er både aia og oppfolging og meldekort nivå 4? Hva med produktkort?
        testFactory(
            safResponse = SafResponse(listOf("DAG"), emptyList())
        ).build(
            microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url") * 5,
                level3Microfrontends = MicrofrontendsDefinition("id", "url") * 2,
            ),
            innloggetnivå = 3,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe true
            produktkort shouldBe listOf("DAG")
            aiaStandardWrapper shouldBe false
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 2
        }
    }
}

private operator fun MicrofrontendsDefinition.times(i: Int): List<MicrofrontendsDefinition> =
    (1..i).map { this.copy(id = "$id$it", url = "$url$it") }

private fun testFactory(
    arbeidsøkerResponse: ArbeidsøkerResponse = ArbeidsøkerResponse(false, false, emptyList()),
    safResponse: SafResponse = SafResponse(emptyList(), emptyList()),
    meldekortResponse: MeldekortResponse = MeldekortResponse(todo = false, errors = emptyList()),
    oppfolgingResponse: OppfolgingResponse = OppfolgingResponse(underOppfolging = false, errors = emptyList())
) =
    PersonalContentFactory(
        arbeidsøkerResponse = arbeidsøkerResponse,
        safResponse = safResponse,
        meldekortResponse = meldekortResponse,
        oppfolgingResponse = oppfolgingResponse
    )

private fun microfrontendMocck(
    level3Microfrontends: List<MicrofrontendsDefinition> = emptyList(),
    level4Microfrontends: List<MicrofrontendsDefinition>? = null,
) = mockk<Microfrontends> {
    every { offerStepup(4) } returns false
    every { offerStepup(3) } returns (level3Microfrontends != level4Microfrontends)
    every { getDefinitions(3, any()) } returns level3Microfrontends
    every { getDefinitions(4, any()) } returns (level4Microfrontends ?: level3Microfrontends)
    every { ids() } returns listOf("mock")
}