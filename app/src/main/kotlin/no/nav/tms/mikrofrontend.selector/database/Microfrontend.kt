package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.applyMigrations
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.toDbNode
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import org.postgresql.util.PGobject


class Microfrontends(initialJson: String? = null) {
    private val log = KotlinLogging.logger { }

    private val originalData: List<JsonNode>? =
        initialJson
            ?.let { microfrontendMapper.readTree(it)["microfrontends"] }
            ?.map { it.applyMigrations() }
            ?.toList()


    private val newData = originalData?.toMutableSet() ?: mutableSetOf()

    companion object {
        val microfrontendMapper = jacksonObjectMapper()
    }

    fun addMicrofrontend(message: JsonMessage): Boolean =
        newData.find { it["microfrontend_id"].asText() == message.microfrontendId }
            ?.let { dbNode ->
                if (message.sensitivitet != dbNode.sensitivitet) {
                    log.info { "Endring av sikkerhetsnivå for ${message.microfrontendId} fra ${dbNode.sensitivitet} til ${message.sensitivitet}" }
                    removeMicrofrontend(message.microfrontendId)
                    newData.add(message.toDbNode())
                } else false
            }
            ?: newData.add(message.toDbNode())
                .also { "Legger til ny mikrofrontend med id ${message.microfrontendId} og sensitivitet ${message.sensitivitet}" }

    fun removeMicrofrontend(microfrontendId: String) =
        newData.removeIf { node ->
            node["microfrontend_id"].asText() == microfrontendId
        }


    fun newDataJsonB(): PGobject = """{ "microfrontends": ${newData.jsonArrayString()}}""".trimMargin().jsonB()
    fun originalDataJsonB(): PGobject? =
        originalData?.let { """{ "microfrontends": ${it.jsonArrayString()}}""".trimMargin() }?.jsonB()

    private fun Collection<JsonNode>.jsonArrayString(): String = joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    )

    private fun String.jsonB() = PGobject().apply {
        type = "jsonb"
        value = this@jsonB
    }

    fun getDefinitions(innloggetnivå: LevelOfAssurance, manifestMap: Map<String, String>): List<MicrofrontendsDefinition> =
        newData
            .filter { Sensitivitet.fromJsonNode(it["sensitivitet"]) <= innloggetnivå }
            .mapNotNull { MicrofrontendsDefinition.create(it["microfrontend_id"].asText(), manifestMap) }

    fun offerStepup(innloggetnivå: LevelOfAssurance): Boolean =
        newData.any { Sensitivitet.fromJsonNode(it["sensitivitet"]) > innloggetnivå }

    fun ids(innloggetnivå: LevelOfAssurance): List<String> = newData.mapNotNull {
        if (Sensitivitet.fromJsonNode(it["sensitivitet"]) >= innloggetnivå)
            it["microfrontend_id"].asText()
        else
            null
    }

}





