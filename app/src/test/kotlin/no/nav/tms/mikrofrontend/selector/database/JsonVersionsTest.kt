package no.nav.tms.mikrofrontend.selector.database

import assert
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.applyMigrations
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.sensitivitet
import objectMapper
import org.junit.jupiter.api.Test

internal class JsonVersionsTest {
    class DatabaseObjectMigrations {

        private val currentFormatHigh = objectMapper.readTree(
            """
             {
                "microfrontend_id": "mfk1",
                "sensitivitet" : "HIGH"
            }
    """.trimIndent()
        )

        private val currentFormatSubstantial = objectMapper.readTree(
            """
             {
                "microfrontend_id": "mfk1",
                "sensitivitet" : "SUBSTANTIAL"
            }
    """.trimIndent()
        )


        private val listeFormat = objectMapper.readTree(
            """
        "mfk1"
    """.trimIndent()
        )

        private val formatWithSikkerhetsnivå4 = objectMapper.readTree(
            """
             {
                "microfrontend_id": "mfk1",
                "sikkerhetsnivå" : "4"
            }
    """.trimIndent()
        )

        val formatWithSikkerhetsnivå3 = objectMapper.readTree(
            """
             {
                "microfrontend_id": "mfk1",
                "sikkerhetsnivå" : "3"
            }
    """.trimIndent()
        )


        @Test
        fun `beholder gjeldende format`() {
            currentFormatHigh.applyMigrations() shouldBe currentFormatHigh
            currentFormatSubstantial shouldBe currentFormatSubstantial
        }

        @Test
        fun `migrerer fra eldre formater`() {
            formatWithSikkerhetsnivå4.applyMigrations().assert {
                this["microfrontend_id"].asText() shouldBe "mfk1"
                this["sensitivitet"].asText() shouldBe "HIGH"
            }
            formatWithSikkerhetsnivå3.applyMigrations().assert {
                this["microfrontend_id"].asText() shouldBe "mfk1"
                this["sensitivitet"].asText() shouldBe "SUBSTANTIAL"
            }

            listeFormat.applyMigrations().assert {
                this["microfrontend_id"].asText() shouldBe "mfk1"
                this["sensitivitet"].asText() shouldBe "HIGH"
            }

        }
    }

    class JsonMessageMigrations {
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
                "sikkerhetsnivå" to 4
            )
        ).applyInterestedIn()
        val version3Message = JsonMessage.newMessage(
            version1Map + mapOf(
                "initiated_by" to "team3",
                "sikkerhetsnivå" to 4
            )
        )
        val version3MessageSensitivitetSubstantial = JsonMessage.newMessage(
            version1Map + mapOf(
                "initiated_by" to "team3",
                "sikkerhetsnivå" to 4
            )
        ).applyInterestedIn()

        @Test
        fun `konverterer sensitvitet riktig`() {
            version1Message.sensitivitet shouldBe Sensitivitet.HIGH
            version2Message.sensitivitet shouldBe Sensitivitet.HIGH
            version2MessageSikkerhetsnivå3.sensitivitet shouldBe Sensitivitet.SUBSTANTIAL
            version3Message.sensitivitet shouldBe Sensitivitet.HIGH
            version3MessageSensitivitetSubstantial shouldBe Sensitivitet.SUBSTANTIAL

        }
    }
}

private fun JsonMessage.applyInterestedIn() = this.apply { interestedIn("sikkerhetsnivå", "sensitivitet") }