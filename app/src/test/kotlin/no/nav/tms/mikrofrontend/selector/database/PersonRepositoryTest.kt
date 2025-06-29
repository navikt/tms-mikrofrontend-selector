package no.nav.tms.mikrofrontend.selector.database

import LocalPostgresDatabase
import dbv1Format
import dbv2Format
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.tms.mikrofrontend.selector.testJsonMessage
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.SUBSTANTIAL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = PersonRepository(database, mockk<MicrofrontendCounter>().also {
        coEvery { it.countMicrofrontendActions(any(), any()) }.answers { }
    })

    @BeforeEach
    fun cleanDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from person") }
    }

    @Test
    fun `Skal sette inn mikrofrontend for ident`() {
        val personIdent = "13499"
        repository.enableMicrofrontend(
            testJsonMessage(
                EnableMessage,
                ident = personIdent,
                microfrontendId = "mkf1",
                initiatedBy = "test-team-1"
            )
        )
        repository.enableMicrofrontend(testJsonMessage(ident = personIdent, microfrontendId = "mkf3", messageRequirements = EnableMessage))
        repository.enableMicrofrontend(testJsonMessage(ident = personIdent, microfrontendId = "mkf3"))
        repository.enableMicrofrontend(
            testJsonMessage(
                microfrontendId = "mkf4",
                ident = personIdent,
                levelOfAssurance = SUBSTANTIAL,
                initiatedBy = "test-team-2"
            )
        )
        repository.enableMicrofrontend(
            testJsonMessage(
                microfrontendId = "mfk5",
                ident = personIdent,
                initiatedBy = "new-team"
            )
        )

        database.getMicrofrontends(personIdent).run {
            shouldNotBeNull()
            size shouldBe 4
            find { it["microfrontend_id"].asText() == "mkf4" }.run {
                shouldNotBeNull()
                withClue("Feil i sikkerhetsnivå for mfk4") { get("sensitivitet")?.asText() shouldBe Sensitivitet.SUBSTANTIAL.kafkaValue }
            }
            find { it["microfrontend_id"].asText() == "mkf1" }.run {
                shouldNotBeNull()
                withClue("Feil i sikkerhetsnivå for mkf1") { get("sensitivitet")?.asText() shouldBe Sensitivitet.HIGH.kafkaValue }
            }
        }
        database.getChangelog(personIdent).run {
            size shouldBe 4
            first().initiatedBy shouldBe "test-team-1"
            last().initiatedBy shouldBe "new-team"
        }

    }

    @Test
    fun `Skal sette inn mikrofrontend for ident som har gamle innslag i tabellen`() {
        val testId1 = "7766"
        database.insertLegacyFormat(ident = testId1, format = ::dbv1Format, "m1", "m2", "m3")
        repository.enableMicrofrontend(testJsonMessage(microfrontendId = "mkf4", ident = testId1, levelOfAssurance = SUBSTANTIAL))
        database.getMicrofrontends(testId1).run {
            shouldNotBeNull()
            size shouldBe 4
        }

        repository.enableMicrofrontend(testJsonMessage(microfrontendId = "mfk6", ident = testId1))
        database.getMicrofrontends(testId1).run {
            shouldNotBeNull()
            map { it["microfrontend_id"].asText() }.forEach { id ->
                id shouldBeIn listOf("m1", "m2", "m3", "mkf4", "mfk6")
                this.size shouldBe 5
                find { it["microfrontend_id"].asText() == "mkf4" }?.get("sensitivitet")
                    ?.asText() shouldBe SUBSTANTIAL.name.lowercase()
                find { it["microfrontend_id"].asText() == "m1" }?.get("sensitivitet")
                    ?.asText() shouldBe LevelOfAssurance.HIGH.name.lowercase()
            }

        }

    }

    @Test
    fun `Skal slette mikfrofrontender `() {
        val testIdent = "1345"
        repository.enableMicrofrontend(
            testJsonMessage(
                microfrontendId = "mkf1",
                ident = testIdent,
                initiatedBy = "test-team-3"
            )
        )
        repository.enableMicrofrontend(testJsonMessage(microfrontendId = "mkf3", ident = testIdent))
        repository.enableMicrofrontend(testJsonMessage(microfrontendId = "mkf4", ident = testIdent, levelOfAssurance = SUBSTANTIAL))
        repository.enableMicrofrontend(testJsonMessage(microfrontendId = "mkf4", ident = testIdent, levelOfAssurance = SUBSTANTIAL))

        require(database.getMicrofrontends(testIdent)!!.size == 3)

        repository.disableMicrofrontend(
            testJsonMessage(
                messageRequirements = DisableMessage,
                ident = testIdent,
                microfrontendId = "mkf3",
                initiatedBy = "test-team5"
            )
        )
        database.getMicrofrontends(testIdent)!!.size shouldBe 2

        repository.disableMicrofrontend(
            testJsonMessage(
                messageRequirements = DisableMessage,
                ident = testIdent,
                microfrontendId = "mkf4",
                initiatedBy = "test-team-4"
            )
        )

        database.getMicrofrontends(testIdent).run {
            shouldNotBeNull()
            size shouldBe 1
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("mkf1")
        }

        database.getChangelog(testIdent).run {
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
        database.insertLegacyFormat(ident = testId1, format = ::dbv1Format, "m1", "m2", "m3")
        repository.disableMicrofrontend(
            testJsonMessage(
                messageRequirements = DisableMessage,
                ident = testId1,
                microfrontendId = "m1",
                initiatedBy = "default-team"
            )
        )

        database.getMicrofrontends(testId1).run {
            shouldNotBeNull()
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }

        database.insertLegacyFormat(ident = testId2, format = ::dbv1Format, "m1", "m2", "m3")
        repository.disableMicrofrontend(
            testJsonMessage(DisableMessage, "mkk", testId2, SUBSTANTIAL, "init-team")
        )

        database.getMicrofrontends(testId1).run {
            shouldNotBeNull()
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }

        database.insertLegacyFormat(ident = testId3, format = ::dbv2Format, "m1", "m2", "m3")
        repository.disableMicrofrontend(
            testJsonMessage(DisableMessage, "mkk", testId3, SUBSTANTIAL, "init-team")
        )

        database.getMicrofrontends(testId1).run {
            shouldNotBeNull()
            size shouldBe 2
            map { it["microfrontend_id"].asText() } shouldContainExactly listOf("m2", "m3")
        }
    }
}


