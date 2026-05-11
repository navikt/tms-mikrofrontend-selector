package no.nav.tms.mikrofrontend.selector.versions

import io.kotest.matchers.shouldBe
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.mikrofrontend.selector.toJsonMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.levelOfAssurance
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance.High
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance.Substantial
import org.junit.jupiter.api.Test

internal class JsonMessageVersionsTest {
    //TODO: fix!
    val version1Map = mutableMapOf(
        "@event_name" to "enable",
        "@action" to "enable",
        "ident" to "12345678910",
        "mikrofrontend_id" to "mp3"
    )
    val version1Message = version1Map.toJsonMessage()
    val version2Message = (
            version1Map + mapOf(
                "initiated_by" to "team3",
                "sikkerhetsnivå" to 4
            )
            ).toJsonMessage()


    val version2MessageSikkerhetsnivå3 = createJsonMessage(
        version1Map,
        "initiated_by" to "team3",
        "sikkerhetsnivå" to 3
    )
    val version3Message = createJsonMessage(
        version1Map,
        "initiated_by" to "team3",
        "sikkerhetsnivå" to 4

    )
    val version3MessageSensitivitetSubstantial = createJsonMessage(
        version1Map,
        "initiated_by" to "team3",
        "sikkerhetsnivå" to 3
    )

    @Test
    fun `konverterer sensitvitet riktig`() {
        version1Message.levelOfAssurance shouldBe High
        version2Message.levelOfAssurance shouldBe High
        version2MessageSikkerhetsnivå3.levelOfAssurance shouldBe Substantial
        version3Message.levelOfAssurance shouldBe High
        version3MessageSensitivitetSubstantial.levelOfAssurance shouldBe Substantial

    }
}

fun <K, V> createJsonMessage(map: Map<out K, V>, vararg pairs: Pair<K, V>): JsonMessage {
    val mutableMap = map.toMutableMap()
    pairs.forEach {
        mutableMap[it.first] = it.second
    }
    return mutableMap.toJsonMessage()
}
