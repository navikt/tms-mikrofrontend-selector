package no.nav.tms.mikrofrontend.selector

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.tms.common.testutils.assert
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.isMissingOrNull
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import no.nav.tms.microfrontend.Sensitivitet as BuilderSensitivitet
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MessageLibraryVerificatiion {

    private val personRepository = mockk<PersonRepository>(relaxed = true)
    private val broadcaster = setupBroadcaster(personRepository)


    @BeforeAll
    fun setupSinks() {
        EnableSubscriber(personRepository)
        DisableSubscriber(personRepository)
    }

    @Disabled
    @Test
    fun `riktige felt i enable`() {
        val jsonMessages = mutableListOf<JsonMessage>()
        coEvery { personRepository.enableMicrofrontend(capture(jsonMessages)) } answers { }

        broadcaster.broadcastJson(
            MicrofrontendMessageBuilder.enable(
                ident = "12345678920",
                microfrontendId = "microf4",
                initiatedBy = "minside",
                sensitivitet = BuilderSensitivitet.HIGH
            ).text()
        )

        broadcaster.broadcastJson(
            MicrofrontendMessageBuilder.enable {
                ident = "12345678920"
                microfrontendId = "microf9"
                initiatedBy = "minside"
                sensitivitet = BuilderSensitivitet.HIGH
            }.text()
        )

        coVerify(exactly = 2) { personRepository.enableMicrofrontend(any()) }

        jsonMessages.first { it.microfrontendId == "microf4" }.assert {
            get("ident").asText() shouldBe "12345678920"
            get("@initiated_by").asText() shouldBe "minside"
            get("sensitivitet").asText() shouldBe Sensitivitet.HIGH.stringValue

            EnableMessage.requiredFields.forEach { expectedKey ->
                withClue("$expectedKey mangler i melding fra messagebuilder") { get(expectedKey).isMissingOrNull() shouldBe false }
            }
        }

    }

    @Disabled
    @Test
    fun `riktige felt i disable`() {
        val jsonMessages = mutableListOf<JsonMessage>()
        coEvery { personRepository.disableMicrofrontend(capture(jsonMessages)) } answers { }

        broadcaster.broadcastJson(
            MicrofrontendMessageBuilder.disable(
                ident = "12345678910",
                microfrontenId = "jjggk",
                initiatedBy = "sdhjkshdfksfh"
            ).text()
        )

        broadcaster.broadcastJson(
            MicrofrontendMessageBuilder.disable {
                ident = "12345678920"
                microfrontendId = "microf9"
                initiatedBy = "minside"
            }.text()
        )

        coVerify(exactly = 2) { personRepository.disableMicrofrontend(any()) }
        DisableMessage.requiredFields.forEach { expectedKey ->
            withClue("$expectedKey mangler i melding fra messagebuilder") { jsonMessages.first()[expectedKey].isMissingOrNull() shouldBe false }
        }

    }

}
