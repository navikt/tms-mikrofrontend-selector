package no.nav.tms.mikrofrontend.selector.versions

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.sensitivitet
import org.junit.jupiter.api.Test

internal class JsonMessageVersionsTest {
    val version1Map = mutableMapOf("@action" to "enable", "ident" to "12345678910", "mikrofrontend_id" to "mp3")
    val version1Message =
        JsonMessage.newMessage(version1Map).applyInterestedIn()
    val version2Message = JsonMessage.newMessage(
        version1Map + mapOf(
            "initiated_by" to "team3",
            "sikkerhetsnivå" to 4
        )
    ).applyInterestedIn()
    val version2MessageSikkerhetsnivå3 = JsonMessage.newMessage(
        version1Map + mapOf(
            "initiated_by" to "team3",
            "sikkerhetsnivå" to 3
        )
    ).applyInterestedIn()
    val version3Message = JsonMessage.newMessage(
        version1Map + mapOf(
            "initiated_by" to "team3",
            "sikkerhetsnivå" to 4
        )
    ).applyInterestedIn()
    val version3MessageSensitivitetSubstantial = JsonMessage.newMessage(
        version1Map + mapOf(
            "initiated_by" to "team3",
            "sikkerhetsnivå" to 3
        )
    ).applyInterestedIn()

    @Test
    fun `konverterer sensitvitet riktig`() {
        version1Message.sensitivitet shouldBe Sensitivitet.HIGH
        version2Message.sensitivitet shouldBe Sensitivitet.HIGH
        version2MessageSikkerhetsnivå3.sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
        version3Message.sensitivitet shouldBe Sensitivitet.HIGH
        version3MessageSensitivitetSubstantial.sensitivitet shouldBe Sensitivitet.SUBSTANTIAL

    }
}

private fun JsonMessage.applyInterestedIn() = this.apply { interestedIn("sikkerhetsnivå", "sensitivitet") }