package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper
import no.nav.tms.mikrofrontend.selector.database.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.levelOfAssurance
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance.High
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance.Substantial

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
            ?: High

    fun JsonNode.applyMigrations(): JsonNode = when {
        isSecondDbVersion -> currentVersionNode(this["microfrontend_id"].asText(), this.levelOfAssurance)
        isFirstDbVersion -> currentVersionNode(asText(), this.levelOfAssurance)
        else -> this
    }
}

object LevelOfAssuranceResolver {
    private infix fun String.correspondsTo(loa: LevelOfAssurance) = lowercase() == loa.name.lowercase()

    fun fromSikkerhetsnivå(sikkerhetsnivå: Int?): LevelOfAssurance = when (sikkerhetsnivå) {
        null -> High
        4 -> High
        3 -> Substantial
        else -> {
            log.error { "$sikkerhetsnivå har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi High" }
            High
        }
    }

    fun fromString(sensitivitetString: String?): LevelOfAssurance = when {
        sensitivitetString == null -> High
        sensitivitetString correspondsTo High -> High
        sensitivitetString correspondsTo Substantial -> Substantial
        else -> {
            log.error { "$sensitivitetString har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi High" }
            High
        }
    }

    fun fromJsonNode(jsonNode: JsonNode) = when {
        jsonNode.isMissingOrNull() -> High
        jsonNode.asText() correspondsTo High -> High
        jsonNode.asText() == "4" -> High
        jsonNode.asText() correspondsTo Substantial -> Substantial
        jsonNode.asText() == "3" -> Substantial
        else -> {
            log.error { "${jsonNode.asText()} har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi High" }
            High
        }
    }
}
