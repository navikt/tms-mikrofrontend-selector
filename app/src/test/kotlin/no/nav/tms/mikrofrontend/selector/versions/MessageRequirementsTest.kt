package no.nav.tms.mikrofrontend.selector.versions

import assert
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.withClue
import io.mockk.clearAllMocks
import io.mockk.clearConstructorMockk
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.LegacyJsonMessages
import no.nav.tms.mikrofrontend.selector.currentVersionPacket
import no.nav.tms.mikrofrontend.selector.ident
import no.nav.tms.mikrofrontend.selector.metrics.MessageVersionCounter
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1


internal class MessageVersionsTest {

    @Test
    fun `setter riktige key requirements for enable`() {
        JsonMessage.newMessage(mapOf("microfrontend_id" to "12358mk", "ident" to "887766")).apply {
            EnableMessage.requireCommonKeys(this)
            EnableMessage.interestedInLegacyKeys(this)
            EnableMessage.interestedInCurrentVersionKeys(this)
        }.assert {
            shouldNotThrow<IllegalArgumentException> {
                microfrontendId
                ident
            }
            listOf("@initiated_by", "initiated_by", "sikkerhetsnivå", "sensitivitet").forEach { key ->
                withClue("interestedIn $key er ikke satt") {
                    shouldNotThrow<IllegalArgumentException> { this[key].asText() }
                }
            }
        }
    }

    @Disabled //mockkConstructor fungerer ikke når alle testene kjøres samtidig
    @Test
    fun `teller riktige versjoner for enable`() {
        mockkConstructor(MessageVersionCounter::class)
        every { anyConstructed<MessageVersionCounter>().countMessageVersion(any(), any(), any()) } answers {}


        EnableMessage.countVersion(LegacyJsonMessages.v1Message("12345", "mk5", EnableMessage))
        DisableMessage.countVersion(LegacyJsonMessages.v1Message("123466", "mk5", DisableMessage))
        EnableMessage.countVersion(LegacyJsonMessages.v1Message("123466", "mk6", EnableMessage))

        verify(exactly = 2) {
            anyConstructed<MessageVersionCounter>().countMessageVersion(
                version = "V1",
                microfrontendId = "mk5",
                team = "NA"
            )
        }
        verify(exactly = 1) {
            anyConstructed<MessageVersionCounter>().countMessageVersion(
                version = "V1",
                microfrontendId = "mk6",
                team = "NA"
            )
        }

        EnableMessage.countVersion(LegacyJsonMessages.enableV2Message("12345", "mk1", "some team", 3))
        EnableMessage.countVersion(LegacyJsonMessages.enableV2Message("12345", "mk2", "some other team", 3))
        DisableMessage.countVersion(LegacyJsonMessages.disableV2Message("12345", "mk3", "some other team"))
        EnableMessage.countVersion(LegacyJsonMessages.enableV2Message("12345", "mk4", "some other team", 4))

        verify(exactly = 3) {
            anyConstructed<MessageVersionCounter>().countMessageVersion(
                version = "V2",
                any(),
                "some other team"
            )
        }
        verify(exactly = 1) { anyConstructed<MessageVersionCounter>().countMessageVersion("V2", "mk1", "some team") }

        DisableMessage.countVersion(
            currentVersionPacket(
                DisableMessage,
                "mk88",
                ident = "8765431",
                Sensitivitet.HIGH,
                "test"
            )
        )
        DisableMessage.countVersion(
            currentVersionPacket(
                DisableMessage,
                "mk88",
                ident = "8765431",
                Sensitivitet.HIGH,
                "test"
            )
        )
        EnableMessage.countVersion(
            currentVersionPacket(
                EnableMessage,
                "mk33",
                ident = "87551",
                Sensitivitet.SUBSTANTIAL,
                "test3"
            )
        )
        EnableMessage.countVersion(
            currentVersionPacket(
                EnableMessage,
                "mk33",
                ident = "87637824431",
                Sensitivitet.HIGH,
                "test3"
            )
        )
        EnableMessage.countVersion(
            currentVersionPacket(
                EnableMessage,
                "mk88",
                ident = "8703931",
                Sensitivitet.SUBSTANTIAL,
                "test2"
            )
        )

        verify(exactly = 2) {
            anyConstructed<MessageVersionCounter>().countMessageVersion(
                version = "V3",
                "mk88",
                "test"
            )
        }
        verify(exactly = 2) {
            anyConstructed<MessageVersionCounter>().countMessageVersion(
                version = "V3",
                "mk33",
                "test3"
            )
        }
        verify(exactly = 1) {
            anyConstructed<MessageVersionCounter>().countMessageVersion(
                version = "V3",
                "mk88",
                "test2"
            )
        }

    }


    @Test
    fun `setter riktige key requirements for disable`() {
        JsonMessage.newMessage(mapOf("microfrontend_id" to "12358mk", "ident" to "887766")).apply {
            DisableMessage.requireCommonKeys(this)
            DisableMessage.interestedInLegacyKeys(this)
            DisableMessage.interestedInCurrentVersionKeys(this)
        }.assert {
            shouldNotThrow<IllegalArgumentException> {
                microfrontendId
                ident
            }
            listOf("@initiated_by", "initiated_by").forEach { key ->
                withClue("interestedIn $key er ikke satt") {
                    shouldNotThrow<IllegalArgumentException> { this[key].asText() }
                }
            }
        }
    }


}