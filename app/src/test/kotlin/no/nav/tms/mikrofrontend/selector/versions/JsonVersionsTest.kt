package no.nav.tms.mikrofrontend.selector.versions

import io.kotest.matchers.shouldBe
import no.nav.tms.common.testutils.assert
import no.nav.tms.mikrofrontend.selector.objectMapper
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.applyMigrations
import org.junit.jupiter.api.Test

internal class JsonVersionsTest {

    private val currentFormatHigh = objectMapper.readTree(
        """
             {
                "microfrontend_id": "mfk1",
                "sensitivitet" : "high"
            }
    """.trimIndent()
    )

    private val currentFormatSubstantial = objectMapper.readTree(
        """
             {
                "microfrontend_id": "mfk1",
                "sensitivitet" : "substantial"
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
        currentFormatHigh.applyMigrations().toPrettyString() shouldBe currentFormatHigh.toPrettyString()
        currentFormatSubstantial.applyMigrations().toPrettyString() shouldBe currentFormatSubstantial.toPrettyString()
    }

    @Test
    fun `migrerer fra eldre formater`() {
        formatWithSikkerhetsnivå4.applyMigrations().assert {
            this["microfrontend_id"].asText() shouldBe "mfk1"
            this["sensitivitet"].asText() shouldBe "high"
        }
        formatWithSikkerhetsnivå3.applyMigrations().assert {
            this["microfrontend_id"].asText() shouldBe "mfk1"
            this["sensitivitet"].asText() shouldBe "substantial"
        }

        listeFormat.applyMigrations().assert {
            this["microfrontend_id"].asText() shouldBe "mfk1"
            this["sensitivitet"].asText() shouldBe "high"
        }

    }
}
