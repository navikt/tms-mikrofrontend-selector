package no.nav.tms.mikrofrontend.selector

import LocalPostgresDatabase
import assert
import dbv1Format
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.prometheus.client.CollectorRegistry
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter

import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SinkTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val personRepository = PersonRepository(
        database = database,
        counter = MicrofrontendCounter()
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
     fun `skal håndtere gjeldene jsonversjon`(){
         val enableMsg = currentVersionMessage(EnableMessage, "tadda-mc", "2456789")
         val disableMsg = currentVersionMessage(DisableMessage, "tadda-mc", "2456789")

        testRapid.sendTestMessage(enableMsg)
        database.getMicrofrontends(ident = "2456789").assert {
            require(this != null)
            size shouldBe 1
        }

        testRapid.sendTestMessage(disableMsg)
        database.getMicrofrontends(ident = "2456789").assert {
            require(this != null)
            size shouldBe 0
        }


     }

    @Test
    fun `Skal enable+oppdatere mikrofrontends og opprette historikk`() {
        val testIdent = "12345678910"
        val oldAndRusty = "old-and-rusty"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"
        val microNewVersion = "current-json-version"

        database.insertLegacyFormat(ident = testIdent, format = ::dbv1Format, oldAndRusty)

        val enableMsg1 = legacyEnabledMessageUtenSikkerhetsnivå(
            ident = testIdent,
            microfrontendId = testmicrofeId1,
            initiatedBy = "testteam"
        )
        val enableMsg2 = legacyMessagev2(microfrontendId = testmicrofeId2, ident = testIdent, initiatedBy = null)
        val enableMsg3 = legacyMessagev2(microfrontendId = testmicrofeId2, ident = testIdent, sikkerhetsnivå = 3)

        testRapid.sendTestMessage(enableMsg1)
        testRapid.sendTestMessage(enableMsg2)
        testRapid.sendTestMessage(enableMsg3)

        testRapid.sendTestMessage(
            currentVersionMessage(
                messageRequirements = EnableMessage,
                microfrontendId = microNewVersion,
                ident = testIdent,
                sensitivitet = Sensitivitet.HIGH,
                initiatedBy = "test-team"
            )
        )

        database.getMicrofrontends(ident = testIdent).assert {
            require(this != null)
            size shouldBe 4
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf(
                oldAndRusty,
                testmicrofeId1,
                testmicrofeId2,
                microNewVersion
            )
            find { it["microfrontend_id"].asText() == testmicrofeId1 }!!
                .get("sensitivitet")?.asText() shouldBe Sensitivitet.HIGH.stringValue
            find { it["microfrontend_id"].asText() == testmicrofeId2 }!!
                .get("sensitivitet")?.asText() shouldBe Sensitivitet.SUBSTANTIAL.stringValue
            find { it["microfrontend_id"].asText() == oldAndRusty }!!.get("sensitivitet")?.asText() shouldBe Sensitivitet.HIGH.stringValue
            find { it["microfrontend_id"].asText() == microNewVersion }!!.get("sensitivitet")
                ?.asText() shouldBe Sensitivitet.HIGH.stringValue
        }

        database.getChangelog(testIdent).assert {
            size shouldBe 4
            get(0).assert {
                originalData shouldContain oldAndRusty
                newData.microfrontendids().size shouldBe 2
                initiatedBy shouldBe "testteam"
            }
            get(1).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 2
                newData.microfrontendids().size shouldBe 3
                initiatedBy shouldBe null
            }
            get(2).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 3
                newData.microfrontendids().size shouldBe 3
                initiatedBy shouldBe "default-team"
            }

            get(3).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 3
                newData.microfrontendids().size shouldBe 4
                initiatedBy shouldBe "test-team"

            }
        }
    }

    @Test
    fun `Skal disable mikrofrontend og oppdatere historikk`() {
        val testFnr = "12345678910"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"

        testRapid.sendTestMessage(
            legacyMessagev2(
                microfrontendId = testmicrofeId1,
                ident = testFnr,
                initiatedBy = "id1team"
            )
        )
        testRapid.sendTestMessage(legacyMessagev2(microfrontendId = testmicrofeId1, ident = "9988776655"))
        testRapid.sendTestMessage(
            legacyMessagev2(
                microfrontendId = testmicrofeId2,
                ident = testFnr,
                initiatedBy = "id2team"
            )
        )

        testRapid.sendTestMessage(
            disableMessage(
                fnr = testFnr,
                microfrontendId = testmicrofeId1,
                initiatedBy = "id1team2"
            )
        )
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
                newData.microfrontendids() shouldContainExactly listOf(testmicrofeId1, testmicrofeId2)
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
        testRapid.sendTestMessage(legacyMessagev2(microfrontendId = testmicrofeId1, ident = testFnr))
        testRapid.sendTestMessage(disableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))
        testRapid.sendTestMessage(legacyMessagev2(microfrontendId = testmicrofeId1, ident = testFnr))

        database.getMicrofrontends(ident = testFnr).assert {
            require(this != null)
            size shouldBe 1
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf(testmicrofeId1)
        }

        database.getChangelog(testFnr).assert {
            size shouldBe 3
        }
    }

    @Test
    fun `fungerer med gammel versjon av initiatedBy`() {
        val enableMsg = legacyEnabledMessageUtenSikkerhetsnivå(
            ident = "12345678910",
            microfrontendId = "testingtesting",
            initiatedBy = "legacy-team"
        )

        testRapid.sendTestMessage(enableMsg)
        database.getMicrofrontends(ident = "12345678910").assert {
            require(this != null)
            size shouldBe 1
            first().assert {
                this["microfrontend_id"].asText() shouldBe "testingtesting"
            }
        }

        database.getChangelog("12345678910").assert {
            size shouldBe 1
            first().initiatedBy shouldBe "legacy-team"
        }
    }
}

private fun String?.microfrontendids(): List<String> {
    require(this != null)
    return objectMapper.readTree(this)["microfrontends"].toList().map { it["microfrontend_id"].asText() }
}


private fun legacyEnabledMessageUtenSikkerhetsnivå(microfrontendId: String, ident: String, initiatedBy: String) = """
    {
      "@action": "enable",
      "ident": "$ident",
      "microfrontend_id": "$microfrontendId",
      "initiated_by":"$initiatedBy"
    }
    """.trimIndent()

private fun disableMessage(microfrontendId: String, fnr: String, initiatedBy: String = "default-team") = """
    {
      "@action": "disable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId",
      "@initiated_by": "$initiatedBy"
}
    """.trimIndent()