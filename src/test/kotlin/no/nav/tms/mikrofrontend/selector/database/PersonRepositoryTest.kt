package no.nav.tms.mikrofrontend.selector.database

import LocalPostgresDatabase
import assert
import enableMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import org.junit.jupiter.api.Test


internal class PersonRepositoryTest {
    private val testDb = LocalPostgresDatabase.cleanDb()
    private val repository = PersonRepository(testDb, mockk<MicrofrontendCounter>().also {
        coEvery { it.countMicrofrontendEnabled(any(),any()) }.answers {  }
    })

    @Test
    fun `Skal sette inn mikrofrontend for ident`() {
        val testId1 = "13499"
        repository.enableMicrofrontend(enablePacket("mkf1", testId1))
        repository.enableMicrofrontend(enablePacket("mkf1", testId1))
        repository.enableMicrofrontend(enablePacket("mkf3", testId1))
        repository.enableMicrofrontend(enablePacket("mkf4", testId1, 3))

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 3
            find { it["microfrontend_id"].asText() == "mkf4" }?.get("sikkerhetsnivå")?.asInt() shouldBe 3
            find { it["microfrontend_id"].asText() == "mkf1" }?.get("sikkerhetsnivå")?.asInt() shouldBe 4
        }
        testDb.getChangelog(testId1).assert {
            size shouldBe 3
            first().initiatedBy shouldBe "test-team-1"
            last().initiatedBy shouldBe "test-team-2"
        }

    }

    @Test
    fun `Skal sette inn mikrofrontend for ident som har gamle innslag i tabellen`() {
        val testId1 = "7766"
        testDb.insertWithLegacyFormat(testId1,"m1","m2","m3")
        repository.enableMicrofrontend(enablePacket("mkf1", testId1))
        repository.enableMicrofrontend(enablePacket("mkf4", testId1, 3))

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 5
            find { it["microfrontend_id"].asText() == "mkf4" }?.get("sikkerhetsnivå")?.asInt() shouldBe 3
            find { it["microfrontend_id"].asText() == "mkf1" }?.get("sikkerhetsnivå")?.asInt() shouldBe 4
        }

    }

    @Test
    fun `Skal slette mikfrofrontender `() {
        val testId1 = "1345"
        repository.enableMicrofrontend(enablePacket("mkf1", testId1))
        repository.enableMicrofrontend(enablePacket("mkf3", testId1))
        repository.enableMicrofrontend(enablePacket("mkf4", testId1, 3))
        repository.enableMicrofrontend(enablePacket("mkf5", testId1, 3))
        repository.disableMicrofrontend(testId1,"mkf3")
        repository.disableMicrofrontend(testId1,"mkf4")

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("mkf1","mkf5")
        }
        testDb.getChangelog(testId1).assert {
            size shouldBe 6
            last().initiatedBy shouldBe "test-team-3"
            first().initiatedBy shouldBe "test-team-4"
        }
    }

    @Test
    fun `Sletter mikrofrontend for ident som har gamle innslag i tabellen`() {
        val testId1 = "7788"
        testDb.insertWithLegacyFormat(testId1,"m1","m2","m3")
        repository.disableMicrofrontend(testId1,"m1")
        repository.disableMicrofrontend(testId1,"mk12")

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2","m3")
        }

        testDb.getChangelog(testId1).size shouldBe 1

    }

}

private fun enablePacket(microfrontendId: String, ident: String, sikkerhetsnivå: Int = 4) =
    JsonMessage(enableMessage(microfrontendId, ident, sikkerhetsnivå), MessageProblems("")).also {
        it.interestedIn("sikkerhetsnivå","microfrontend_id", "ident")
    }

