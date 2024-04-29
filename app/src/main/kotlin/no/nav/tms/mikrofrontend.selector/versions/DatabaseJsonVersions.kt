package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.sensitivitet
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance

private val log = KotlinLogging.logger { }


object DatabaseJsonVersions {
    private val JsonNode.isSecondDbVersion
        get() = this["sikkerhetsnivå"] != null

    private val JsonNode.isFirstDbVersion
        get() = isValueNode

    fun currentVersionNode(id: String, sensitivitet: Sensitivitet): JsonNode = microfrontendMapper.readTree(
        """
         {
            "microfrontend_id": "$id",
            "sensitivitet" : "${sensitivitet.stringValue}"
        }
      """.trimMargin()
    )

    fun JsonMessage.toDbNode() =
        currentVersionNode(microfrontendId, sensitivitet)

    val JsonNode.sensitivitet: Sensitivitet
        get() = this["sensitivitet"]?.let { name -> Sensitivitet.fromString(name.asText()) }
            ?: this["sikkerhetsnivå"]?.let { nivå -> Sensitivitet.fromSikkerhetsnivå(nivå.asInt()) }
            ?: Sensitivitet.HIGH

    fun JsonNode.applyMigrations(): JsonNode = when {
        isSecondDbVersion -> currentVersionNode(this["microfrontend_id"].asText(), this.sensitivitet)
        isFirstDbVersion -> currentVersionNode(asText(), Sensitivitet.HIGH)
        else -> this
    }
}


enum class Sensitivitet(private val sikkerhetsnivå: Int) {
    HIGH(4), SUBSTANTIAL(3);

    val stringValue = name.lowercase()
    operator fun compareTo(innloggetnivå: Int): Int =
        sikkerhetsnivå - fromSikkerhetsnivå(innloggetnivå).sikkerhetsnivå

    companion object {
        fun fromSikkerhetsnivå(sikkerhetsnivå: Int?) = when (sikkerhetsnivå) {
            null -> HIGH
            4 -> HIGH
            3 -> SUBSTANTIAL
            else -> {
                log.error { "$sikkerhetsnivå har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
                HIGH
            }
        }

        fun fromString(sensitivitetString: String?) = when (sensitivitetString) {
            null -> HIGH
            HIGH.stringValue -> HIGH
            SUBSTANTIAL.stringValue -> SUBSTANTIAL
            else -> {
                log.error { "$sensitivitetString har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
                HIGH
            }
        }

        fun fromJsonNode(jsonNode: JsonNode) = when {
            jsonNode.isMissingOrNull() -> LevelOfAssurance.HIGH
            jsonNode.asText() == HIGH.stringValue -> LevelOfAssurance.HIGH
            jsonNode.asText() == "4" -> LevelOfAssurance.HIGH
            jsonNode.asText() == SUBSTANTIAL.stringValue -> LevelOfAssurance.SUBSTANTIAL
            jsonNode.asText() == "3" -> LevelOfAssurance.SUBSTANTIAL
            else -> {
                log.error { "${jsonNode.asText()} har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
                LevelOfAssurance.HIGH
            }
        }


    }
}
