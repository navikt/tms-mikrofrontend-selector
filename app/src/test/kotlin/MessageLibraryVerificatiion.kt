
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.microfrontend.MessageBuilder
import no.nav.tms.microfrontend.Sikkerhetsnivå
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.microfrontendId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MessageLibraryVerificatiion {

    private val personRepository = mockk<PersonRepository>(relaxed = true)
    private val testRapid = TestRapid()


    @BeforeAll
    fun setupSinks() {
        EnableSink(testRapid, personRepository)
        DisableSink(testRapid, personRepository)
    }


    @Test
    fun `riktige felt i enable`() {
        val jsonMessages = mutableListOf<JsonMessage>()
        coEvery { personRepository.enableMicrofrontend(capture(jsonMessages)) } answers { }

        testRapid.sendTestMessage(
            MessageBuilder.enable(
                ident = "12345678920",
                microfrontendId = "microf4",
                initiatedBy = "minside",
                sikkerhetsnivå = Sikkerhetsnivå.NIVÅ_4
            ).text()
        )

        testRapid.sendTestMessage(
            MessageBuilder.enable {
                ident = "12345678920"
                microfrontendId= "microf9"
                initiatedBy = "minside"
                sikkerhetsnivå = Sikkerhetsnivå.NIVÅ_4
            }.text()
        )

        coVerify(exactly = 2) { personRepository.enableMicrofrontend(any()) }

        jsonMessages.first { it.microfrontendId == "microf4" }.assert {
            get("ident").asText() shouldBe "12345678920"
            get("initiated_by").asText() shouldBe "minside"
            get("sikkerhetsnivå").asInt() shouldBe 4
        }
    }

    @Test
    fun `riktige felt i disable`() {
        testRapid.sendTestMessage(
            MessageBuilder.disable(
               ident = "12345678910",
               microfrontenId = "jjggk",
               initiatedBy = "sdhjkshdfksfh"
            ).text()
        )

        testRapid.sendTestMessage(
            MessageBuilder.disable {
                ident = "12345678920"
                microfrontendId= "microf9"
                initiatedBy = "minside"
            }.text()
        )

        coVerify(exactly = 1) { personRepository.disableMicrofrontend("12345678910","jjggk", "sdhjkshdfksfh") }
        coVerify(exactly = 1) { personRepository.disableMicrofrontend("12345678920","microf9", "minside") }
    }

}
