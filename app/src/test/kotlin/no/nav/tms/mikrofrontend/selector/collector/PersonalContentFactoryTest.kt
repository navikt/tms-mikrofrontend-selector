package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.tms.common.testutils.assert
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.HIGH
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.SUBSTANTIAL
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PersonalContentFactoryTest {
    @Test
    fun `Skal være tom`() {
        testFactory().build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandard shouldBe false
            brukNyAia shouldBe false
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha microfrontends og produktkort innlogingsnivå 4`() {
        testFactory(
            safResponse = SafResponse(listOf(Dokument("DAG", LocalDateTime.now()))),
            digisosResponse = DigisosResponse(listOf(Dokument(sakstemakode = "KOM", LocalDateTime.now())))
        ).build(
            microfrontends = microfrontendMocck(level4Microfrontends = MicrofrontendsDefinition("id", "url") * 5),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe listOf("DAG", "KOM")
            aiaStandard shouldBe false
            brukNyAia shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 5
        }
    }

    @Test
    fun `skal ha aia-standard og returstatus 207 pga SAF`() {
        testFactory(
            safResponse = SafResponse(emptyList(), listOf("Saf feilet fordi det gikk feil")),
            arbeidsøkerResponse = ArbeidsøkerResponse(
                erArbeidssoker = true,
                erStandard = true,
                brukNyAia = false
            )
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandard shouldBe true
            brukNyAia shouldBe false
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.MultiStatus
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha oppfolgingcontent`() {
        testFactory(
            oppfolgingResponse = OppfolgingResponse(true)

        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe true
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandard shouldBe false
            brukNyAia shouldBe false
            oppfolgingContent shouldBe true
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends shouldBe emptyList()
        }
    }

    @Test
    fun `skal ha produkkort og aia-standard og 207 pga meldekort`() {
        testFactory(
            arbeidsøkerResponse = ArbeidsøkerResponse(
                erArbeidssoker = true,
                erStandard = true,
                brukNyAia = false
            ),
            meldekortResponse = MeldekortResponse(errors = "Feil som skjedde")

        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe false
            produktkort shouldBe emptyList()
            aiaStandard shouldBe true
            brukNyAia shouldBe false
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.MultiStatus
            this.microfrontends shouldBe emptyList()
        }

    }

    @Test
    fun `skal ha aia-legacy`() {
        testFactory(
            arbeidsøkerResponse = ArbeidsøkerResponse(
                erArbeidssoker = true,
                erStandard = false,
                brukNyAia = false
            ),
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            aiaLegacy shouldBe true
        }

    }

    @Test
    fun `skal ikke ha aia-legacy`() {
        testFactory(
            arbeidsøkerResponse = ArbeidsøkerResponse(
                erArbeidssoker = false,
                erStandard = false,
                brukNyAia = false
            ),
        ).build(
            microfrontends = Microfrontends(),
            levelOfAssurance = HIGH,
            manifestMap = emptyMap()
        ).assert {
            aiaLegacy shouldBe false
        }

    }

    @Test
    fun `skal ha produkkort, ny-aia, oppfolging, meldekort og microfrontends`() {
        //TODO
        testFactory(
            arbeidsøkerResponse = ArbeidsøkerResponse(erArbeidssoker = true, erStandard = true, brukNyAia = false),
                safResponse = SafResponse(
            dokumenter = listOf(Dokument("DAG", LocalDateTime.now())),
            errors = emptyList()
        ),
        meldekortResponse = MeldekortResponse(JsonPathInterpreter.initPathInterpreter("{}")),
        oppfolgingResponse = OppfolgingResponse(underOppfolging = true),
        ).build(
        microfrontends = microfrontendMocck(
            level4Microfrontends = MicrofrontendsDefinition("id", "url") * 5,
            ids = listOf("aia-ny")

        ),
        levelOfAssurance = HIGH,
        manifestMap = mapOf("regefrontend" to "https://micro.moc")
        ).assert {
            oppfolgingContent shouldBe true
            offerStepup shouldBe false
            produktkort shouldBe listOf("DAG")
            aiaStandard shouldBe false
            brukNyAia shouldBe true
            oppfolgingContent shouldBe true
            resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 5
        }

    }

    @Test
    fun `skal ha microfrontends og produktkort innloggingsnivå 3`() {
        //er både aia og oppfolging og meldekort nivå 4? Hva med produktkort?
        testFactory(
            safResponse = SafResponse(
                listOf(Dokument("DAG", LocalDateTime.now())),
                emptyList()
            )
        ).build(
            microfrontendMocck(
                level4Microfrontends = MicrofrontendsDefinition("id", "url") * 5,
                level3Microfrontends = MicrofrontendsDefinition("id", "url") * 2,
            ),
            levelOfAssurance = SUBSTANTIAL,
            manifestMap = emptyMap()
        ).assert {
            oppfolgingContent shouldBe false
            offerStepup shouldBe true
            produktkort shouldBe listOf("DAG")
            aiaStandard shouldBe false
            brukNyAia shouldBe false
            oppfolgingContent shouldBe false
            this.resolveStatus() shouldBe HttpStatusCode.OK
            this.microfrontends.size shouldBe 2
        }
    }
}

private operator fun MicrofrontendsDefinition.times(i: Int): List<MicrofrontendsDefinition> =
    (1..i).map { MicrofrontendsDefinition(id = "$id$it", url = "$url$it") }


private fun testFactory(
    arbeidsøkerResponse: ArbeidsøkerResponse = ArbeidsøkerResponse(false, false, false),
    safResponse: SafResponse = SafResponse(emptyList(), emptyList()),
    meldekortResponse: MeldekortResponse = MeldekortResponse(JsonPathInterpreter.initPathInterpreter("{meldekort:0}")),
    oppfolgingResponse: OppfolgingResponse = OppfolgingResponse(underOppfolging = false),
    pdlResponse: PdlResponse = PdlResponse(LocalDate.parse("1988-09-08"), 1988),
    digisosResponse: DigisosResponse = DigisosResponse()
) =
    PersonalContentFactory(
        arbeidsøkerResponse = arbeidsøkerResponse,
        safResponse = safResponse,
        meldekortResponse = meldekortResponse,
        oppfolgingResponse = oppfolgingResponse,
        pdlResponse = pdlResponse,
        digisosResponse = digisosResponse
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
