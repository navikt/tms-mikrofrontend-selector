package selector

import LocalPostgresDatabase
import assert
import disableMessage
import enableMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.prometheus.client.CollectorRegistry
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SinkTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val personRepository = PersonRepository(
        database = database,
        metricsRegistry = MicrofrontendCounter()
    )
    private val testRapid = TestRapid()


    @BeforeAll
    fun setupSinks() {
        CollectorRegistry.defaultRegistry.clear()
        EnableSink(testRapid, personRepository)
        DisableSink(testRapid, personRepository)
    }

    @AfterEach
    fun cleanDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from person") }
    }

    @Test
    fun `Skal enable+oppdatere mikrofrontends og opprette historikk`() {
        val testIdent = "12345678910"
        val oldAndRusty = "old-and-rusty"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"

        database.insertWithLegacyFormat(testIdent, oldAndRusty)

        val enableMsg1 = enableMessageUtenSikkerhetsnivå(ident = testIdent, microfrontendId = testmicrofeId1, initiatedBy="testteam")
        val enableMsg2 = enableMessage(microfrontendId = testmicrofeId2, fnr = testIdent, initiatedBy = null)
        val enableMsg3 = enableMessage(microfrontendId = testmicrofeId2, fnr = testIdent, sikkerhetsnivå = 3)

        testRapid.sendTestMessage(enableMsg1)
        testRapid.sendTestMessage(enableMsg2)
        testRapid.sendTestMessage(enableMsg3)

        database.getMicrofrontends(ident = testIdent).assert {
            require(this != null)
            size shouldBe 3
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf(
                oldAndRusty,
                testmicrofeId1,
                testmicrofeId2
            )
            find { it["microfrontend_id"].asText() == testmicrofeId1 }.assert {  }!!.get("sikkerhetsnivå")?.asInt() shouldBe 4
            find { it["microfrontend_id"].asText() == testmicrofeId2 }!!.get("sikkerhetsnivå")?.asInt() shouldBe 3
            find { it["microfrontend_id"].asText() == oldAndRusty }!!.get("sikkerhetsnivå")?.asInt() shouldBe 4
        }

        database.getChangelog(testIdent).assert {
            size shouldBe 3
            get(0).assert {
                originalData shouldContain oldAndRusty
                newData.microfrontendids().size shouldBe 2
                initiatedBy shouldBe "testteam"
            }
            get(1).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 2
                newData.microfrontendids().size shouldBe 3
                initiatedBy shouldBe ""
            }
            get(2).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 3
                newData.microfrontendids().size shouldBe 3
                initiatedBy shouldBe "default-team"
            }
        }
    }

    @Test
    fun `Skal disable mikrofrontend og oppdatere historikk`() {
        val testFnr = "12345678910"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"

        testRapid.sendTestMessage(enableMessage(microfrontendId = testmicrofeId1, fnr = testFnr, initiatedBy = "id1team"))
        testRapid.sendTestMessage(enableMessage(microfrontendId = testmicrofeId1, fnr = "9988776655"))
        testRapid.sendTestMessage(enableMessage(microfrontendId = testmicrofeId2, fnr = testFnr, initiatedBy = "id2team"))

        testRapid.sendTestMessage(disableMessage(fnr = testFnr, microfrontendId = testmicrofeId1, initiatedBy = "id1team2"))
        testRapid.sendTestMessage(disableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))

        database.getMicrofrontends(ident = testFnr).assert {
            require(this != null)
            size shouldBe 1
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf(testmicrofeId2)
        }
        database.getChangelog(testFnr).assert {
            size shouldBe 3
            get(0).assert {
                originalData shouldBe null
                newData.microfrontendids() shouldContainExactly listOf(testmicrofeId1)
                initiatedBy shouldBe "id1team"
            }
            get(1).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 1
                newData.microfrontendids() shouldContainExactly listOf(testmicrofeId1,testmicrofeId2)
                initiatedBy shouldBe "id2team"
            }
            get(2).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 2
                newData.microfrontendids().size shouldBe 1
                initiatedBy shouldBe "id1team2"
            }
        }
    }

    @Test
    fun `Skal kunne re-enable mikrofrontend`() {
        val testFnr = "12345678910"
        val testmicrofeId1 = "same-same-but-different"
        testRapid.sendTestMessage(enableMessage(microfrontendId = testmicrofeId1, fnr = testFnr))
        testRapid.sendTestMessage(disableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))
        testRapid.sendTestMessage(enableMessage(microfrontendId = testmicrofeId1, fnr = testFnr))

        database.getMicrofrontends(ident = testFnr).assert {
            require(this != null)
            size shouldBe 1
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf(testmicrofeId1)
        }

        database.getChangelog(testFnr).assert {
            size shouldBe 3
        }
    }
}

private fun String?.microfrontendids(): List<String> {
    require(this != null)
    return objectMapper.readTree(this)["microfrontends"].toList().map { it["microfrontend_id"].asText() }
}


private fun enableMessageUtenSikkerhetsnivå(microfrontendId: String, ident: String, initiatedBy: String) = """
    {
      "@action": "enable",
      "ident": "$ident",
      "microfrontend_id": "$microfrontendId",
      "@initiated_by":"$initiatedBy"
    }
    """.trimIndent()