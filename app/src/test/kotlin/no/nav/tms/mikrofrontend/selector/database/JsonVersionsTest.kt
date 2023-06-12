package no.nav.tms.mikrofrontend.selector.database

import assert
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.migrateToLatestVersion
import org.junit.jupiter.api.Test

internal class JsonVersionsTest {

    private val objectMapper = jacksonObjectMapper()


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
        currentFormatHigh.migrateToLatestVersion() shouldBe currentFormatHigh
        currentFormatSubstantial shouldBe currentFormatSubstantial
    }

    @Test
    fun `migrerer fra eldre formater`() {
        formatWithSikkerhetsnivå4.migrateToLatestVersion().assert {
            this["microfrontend_id"].asText() shouldBe "mfk1"
            this["sensitivitet"].asText() shouldBe "HIGH"
        }
        formatWithSikkerhetsnivå3.migrateToLatestVersion().assert {
            this["microfrontend_id"].asText() shouldBe "mfk1"
            this["sensitivitet"].asText() shouldBe "SUBSTANTIAL"
        }

        listeFormat.migrateToLatestVersion().assert {
            this["microfrontend_id"].asText() shouldBe "mfk1"
            this["sensitivitet"].asText() shouldBe "HIGH"
        }

    }
}