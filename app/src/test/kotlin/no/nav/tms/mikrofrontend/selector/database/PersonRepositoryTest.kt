package no.nav.tms.mikrofrontend.selector.database

import LegacyJsonMessages.disableV2Message
import LegacyJsonMessages.enableV2Message
import LegacyJsonMessages.v1Message
import LocalPostgresDatabase
import assert
import currentVersionPacket
import dbv1Format
import dbv2Format
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.HIGH
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.SUBSTANTIAL
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest {
    private val testDb = LocalPostgresDatabase.cleanDb()
    private val repository = PersonRepository(testDb, mockk<MicrofrontendCounter>().also {
        coEvery { it.countMicrofrontendActions(any(), any()) }.answers { }
    })

    @Test
    fun `Skal sette inn mikrofrontend for ident`() {
        val personIdent = "13499"
        repository.enableMicrofrontend(
            enableV2Message(
                ident = personIdent,
                microfrontendId = "mkf1",
                initiatedBy = "test-team-1"
            )
        )
        repository.enableMicrofrontend(v1Message(personIdent, "mkf3", JsonVersions.EnableKeys))
        repository.enableMicrofrontend(enableV2Message(personIdent, "mkf3"))
        repository.enableMicrofrontend(
            enableV2Message(
                microfrontendId = "mkf4",
                ident = personIdent,
                sikkerhetsnivå = 3,
                initiatedBy = "test-team-2"
            )
        )
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
            find { it["microfrontend_id"].asText() == "mkf4" }.assert {
                require(this != null)
                withClue("Feil i sikkerhetsnivå for mfk4") { get("sensitivitet")?.asText() shouldBe SUBSTANTIAL.name }
            }
            find { it["microfrontend_id"].asText() == "mkf1" }.assert {
                require(this != null)
                withClue("Feil i sikkerhetsnivå for mkf1") { get("sensitivitet")?.asText() shouldBe HIGH.name }
            }
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
        testDb.insertLegacyFormat(ident = testId1, format = ::dbv1Format, "m1", "m2", "m3")
        repository.enableMicrofrontend(enableV2Message(microfrontendId = "mkf4", ident = testId1, sikkerhetsnivå = 3))
        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 4
        }

        repository.enableMicrofrontend(currentVersionPacket(microfrontendId = "mfk6", ident = testId1))
        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            map { it["microfrontend_id"].asText() }.forEach { id ->
                id shouldBeIn listOf("m1", "m2", "m3", "mkf4", "mfk6")
                this.size shouldBe 5
                find { it["microfrontend_id"].asText() == "mkf4" }?.get("sensitivitet")
                    ?.asText() shouldBe SUBSTANTIAL.name
                find { it["microfrontend_id"].asText() == "m1" }?.get("sensitivitet")?.asText() shouldBe HIGH.name
            }

        }

    }

    @Test
    fun `Skal slette mikfrofrontender `() {
        val testIdent = "1345"
        repository.enableMicrofrontend(
            currentVersionPacket(
                microfrontendId = "mkf1",
                ident = testIdent,
                initatedBy = "test-team-3"
            )
        )
        repository.enableMicrofrontend(currentVersionPacket(microfrontendId = "mkf3", ident = testIdent))
        repository.enableMicrofrontend(enableV2Message(microfrontendId = "mkf4", ident = testIdent, sikkerhetsnivå = 3))
        repository.enableMicrofrontend(enableV2Message(microfrontendId = "mkf4", ident = testIdent, sikkerhetsnivå = 3))

        require(testDb.getMicrofrontends(testIdent)!!.size == 3)

        repository.disableMicrofrontend(
            currentVersionPacket(
                keyRequirements = JsonVersions.DisableKeys,
                ident = testIdent,
                microfrontendId = "mkf3",
                initatedBy = "test-team5"
            )
        )
        testDb.getMicrofrontends(testIdent)!!.size shouldBe 2

        repository.disableMicrofrontend(
            currentVersionPacket(
                keyRequirements = JsonVersions.DisableKeys,
                ident = testIdent,
                microfrontendId = "mkf4",
                initatedBy = "test-team-4"
            )
        )

        testDb.getMicrofrontends(testIdent).assert {
            require(this != null)
            size shouldBe 1
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("mkf1")
        }
        testDb.getChangelog(testIdent).assert {
            size shouldBe 5
            first().initiatedBy shouldBe "test-team-3"
            last().initiatedBy shouldBe "test-team-4"
        }
    }

    @Test
    fun `Sletter mikrofrontend for ident som har gamle innslag i tabellen`() {
        val testId1 = "7788"
        val testId2 = "77882"
        val testId3 = "77882"
        testDb.insertLegacyFormat(ident = testId1, format = ::dbv1Format, "m1", "m2", "m3")
        repository.disableMicrofrontend(
            disableV2Message(
                ident = testId1,
                microfrontendId = "m1",
                initiatedBy = "default-team"
            )
        )

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }

        testDb.insertLegacyFormat(ident = testId2, format = ::dbv1Format, "m1", "m2", "m3")
        repository.disableMicrofrontend(
            currentVersionPacket(JsonVersions.DisableKeys, "mkk", testId2, SUBSTANTIAL, "init-team")
        )

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }

        testDb.insertLegacyFormat(ident = testId3, format = ::dbv2Format, "m1", "m2", "m3")
        repository.disableMicrofrontend(
            currentVersionPacket(JsonVersions.DisableKeys, "mkk", testId3, SUBSTANTIAL, "init-team")
        )

        testDb.getMicrofrontends(testId1).assert {
            require(this != null)
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }
    }
}


