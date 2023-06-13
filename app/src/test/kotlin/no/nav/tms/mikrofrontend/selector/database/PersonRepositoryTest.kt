package no.nav.tms.mikrofrontend.selector.database

import LocalPostgresDatabase
import assert
import currentVersionMap
import legacyMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.*
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest {
    private val testDb = LocalPostgresDatabase.cleanDb()
    private val repository = PersonRepository(testDb, mockk<MicrofrontendCounter>().also {
        coEvery { it.countMicrofrontendEnabled(any(), any()) }.answers { }
    })

    @Test
    fun `Skal sette inn mikrofrontend for ident`() {
        val personIdent = "13499"
        repository.enableMicrofrontend(legacyEnablePacket("mkf1", personIdent, initatedBy = "test-team-1"))
        repository.enableMicrofrontend(legacyEnablePacket("mkf1", personIdent, initatedBy = "test-team-2"))
        repository.enableMicrofrontend(legacyEnablePacket("mkf3", personIdent))
        repository.enableMicrofrontend(legacyEnablePacket("mkf4", personIdent, 3, "test-team-2"))
        repository.enableMicrofrontend(
            currentVersionPacket(
                microfrontendId = "mfk5",
                ident = personIdent,
                initatedBy = "new-team"
            )
        )

        testDb.getMicrofrontends(personIdent).assert {
            require(this != null)
            size shouldBe 4
            find { it["microfrontend_id"].asText() == "mkf4" }?.get("sensitivitet")?.asText() shouldBe SUBSTANTIAL.name
            find { it["microfrontend_id"].asText() == "mkf1" }?.get("sensitivitet")?.asText() shouldBe HIGH.name
        }
        testDb.getChangelog(personIdent).assert {
            size shouldBe 4
            first().initiatedBy shouldBe "test-team-1"
            last().initiatedBy shouldBe "new-team"
        }

    }

    @Test
    fun `Skal sette inn mikrofrontend for ident som har gamle innslag i tabellen`() {
        val testId1 = "7766"
        testDb.insertWithLegacyFormat(testId1, "m1", "m2", "m3")
        repository.enableMicrofrontend(legacyEnablePacket("mkf1", testId1))
        repository.enableMicrofrontend(legacyEnablePacket("mkf4", testId1, 3))

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 5
            find { it["microfrontend_id"].asText() == "mkf4" }?.get("sensitivitet")?.asText() shouldBe SUBSTANTIAL.name
            find { it["microfrontend_id"].asText() == "mkf1" }?.get("sensitivitet")?.asText() shouldBe HIGH.name
        }

    }

    @Test
    fun `Skal slette mikfrofrontender `() {
        val testIdent = "1345"
        repository.enableMicrofrontend(legacyEnablePacket("mkf1", testIdent, initatedBy = "test-team-3"))
        repository.enableMicrofrontend(legacyEnablePacket("mkf3", testIdent))
        repository.enableMicrofrontend(legacyEnablePacket("mkf4", testIdent, 3))
        repository.enableMicrofrontend(legacyEnablePacket("mkf5", testIdent, 3))

        repository.disableMicrofrontend(testIdent, "mkf3", "test-team5")
        repository.disableMicrofrontend(testIdent, "mkf4", "test-team-4")

        testDb.getMicrofrontends(testIdent).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("mkf1", "mkf5")
        }
        testDb.getChangelog(testIdent).assert {
            size shouldBe 6
            first().initiatedBy shouldBe "test-team-3"
            last().initiatedBy shouldBe "test-team-4"
        }
    }

    @Test
    fun `Sletter mikrofrontend for ident som har gamle innslag i tabellen`() {
        val testId1 = "7788"
        testDb.insertWithLegacyFormat(testId1, "m1", "m2", "m3")
        repository.disableMicrofrontend(testId1, "m1", "default-team")
        repository.disableMicrofrontend(testId1, "mk12", "default-team")

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }
    }

}

private fun legacyEnablePacket(
    microfrontendId: String,
    ident: String,
    sikkerhetsnivå: Int = 4,
    initatedBy: String? = null,
) =
    JsonMessage(
        legacyMessage(microfrontendId, ident, sikkerhetsnivå, initatedBy ?: "default-team"),
        MessageProblems("")
    )
        .also { message ->
            JsonVersions.Enabled.setRequiredKeys(message)
            JsonVersions.Enabled.setInterestedInKeys(message)
        }

private fun currentVersionPacket(
    action: String = "enable",
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initatedBy: String = "default-team"
) =
    JsonMessage.newMessage(
        currentVersionMap(action, microfrontendId, ident, sensitivitet, initatedBy)
    ).also { jsonMessage ->
        if (action == "enable") {
            JsonVersions.Enabled.setRequiredKeys(jsonMessage)
            JsonVersions.Enabled.setInterestedInKeys(jsonMessage)
        } else {
            JsonVersions.Disabled.setRequiredKeys(jsonMessage)
            JsonVersions.Disabled.setInterestedInKeys(jsonMessage)
        }
    }



