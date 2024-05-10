package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper
import no.nav.tms.mikrofrontend.selector.database.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.levelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.HIGH
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.SUBSTANTIAL

private val log = KotlinLogging.logger { }


object DatabaseJsonVersions {
    private val JsonNode.isSecondDbVersion
        get() = this["sikkerhetsnivå"] != null

    private val JsonNode.isFirstDbVersion
        get() = isValueNode

    fun currentVersionNode(id: String, levelOfAssurance: LevelOfAssurance): JsonNode = microfrontendMapper.readTree(
        """
         {
            "microfrontend_id": "$id",
            "sensitivitet" : "${levelOfAssurance.name.lowercase()}"
        }
      """.trimMargin()
    )

    fun JsonMessage.toDbNode() =
        currentVersionNode(microfrontendId, levelOfAssurance)

    val JsonNode.levelOfAssurance: LevelOfAssurance
        get() = this["sensitivitet"]?.let { name -> LevelOfAssuranceResolver.fromString(name.asText()) }
            ?: this["sikkerhetsnivå"]?.let { nivå -> LevelOfAssuranceResolver.fromSikkerhetsnivå(nivå.asInt()) }
            ?: HIGH

    fun JsonNode.applyMigrations(): JsonNode = when {
        isSecondDbVersion -> currentVersionNode(this["microfrontend_id"].asText(), this.levelOfAssurance)
        isFirstDbVersion -> currentVersionNode(asText(), this.levelOfAssurance)
        else -> this
    }
}

object LevelOfAssuranceResolver {
    private infix fun String.correspondsTo(loa: LevelOfAssurance) = lowercase() == loa.name.lowercase()

    fun fromSikkerhetsnivå(sikkerhetsnivå: Int?): LevelOfAssurance = when (sikkerhetsnivå) {
        null -> HIGH
        4 -> HIGH
        3 -> SUBSTANTIAL
        else -> {
            log.error { "$sikkerhetsnivå har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
            HIGH
        }
    }

    fun fromString(sensitivitetString: String?): LevelOfAssurance = when {
        sensitivitetString == null -> HIGH
        sensitivitetString correspondsTo HIGH -> HIGH
        sensitivitetString correspondsTo SUBSTANTIAL -> SUBSTANTIAL
        else -> {
            log.error { "$sensitivitetString har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
            HIGH
        }
    }

    fun fromJsonNode(jsonNode: JsonNode) = when {
        jsonNode.isMissingOrNull() -> HIGH
        jsonNode.asText() correspondsTo HIGH -> HIGH
        jsonNode.asText() == "4" -> HIGH
        jsonNode.asText() correspondsTo SUBSTANTIAL -> SUBSTANTIAL
        jsonNode.asText() == "3" -> SUBSTANTIAL
        else -> {
            log.error { "${jsonNode.asText()} har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
            HIGH
        }
    }
}
