package selector

import LocalPostgresDatabase
import assert
import enableMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.MicrofrontendRepository
import objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class SinkTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val microfrontendRepository = MicrofrontendRepository(database)
    private val testRapid = TestRapid()

    @BeforeAll
    fun setupSinks() {
        EnableSink(testRapid, microfrontendRepository)
        DisableSink(testRapid, microfrontendRepository)
    }

    @Test
    fun `Skal enable ny mikrofrontend og opprette historikk`() {
        val testFnr = "12345678910"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"

        testRapid.sendTestMessage(enableMessage(fnr = testFnr, mikrofrontendId = testmicrofeId1))
        testRapid.sendTestMessage(enableMessage(fnr = testFnr, mikrofrontendId = testmicrofeId2))

        microfrontendRepository.getEnabledMicrofrontends(fnr = testFnr)["mikrofrontends"].toList().assert {
            size shouldBe 2
            map { it.asText() } shouldContainExactly listOf(testmicrofeId1, testmicrofeId2)
        }
        database.getChangelog(testFnr).assert {
            size shouldBe 1
            first().assert {
                objectMapper.readTree(oldData)["microfrontends"]
            }
        }
    }

    @Test
    fun `Skal disable mikrofrontend og oppdatere historikk`() {
        //TODO
    }

    @Test
    fun `Skal enable ekisisterende mikrofrontend og oppdatere historikk`() {
        //TODO
    }

    @Test
    fun `Ignorerer meldinger om endring som ikke endrer state`() {
        //TODO
    }
}
