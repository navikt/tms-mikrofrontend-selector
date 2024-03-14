package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.applyMigrations
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.toDbNode
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import org.postgresql.util.PGobject

private val objectMapper = jacksonObjectMapper()

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
        fun emptyApiResponse(): String = """{ "microfrontends":[], "offerStepup": false }"""
    }

    fun addMicrofrontend(packet: JsonMessage): Boolean =
        newData.find { it["microfrontend_id"].asText() == packet.microfrontendId }
            ?.let { dbNode ->
                if (packet.sensitivitet != dbNode.sensitivitet) {
                    log.info { "Endring av sikkerhetsnivå for ${packet.microfrontendId} fra ${dbNode.sensitivitet} til ${packet.sensitivitet}" }
                    removeMicrofrontend(packet.microfrontendId)
                    newData.add(packet.toDbNode())
                } else false
            }
            ?: newData.add(packet.toDbNode())
                .also { "Legger til ny mikrofrontend med id ${packet.microfrontendId} og sensitivitet ${packet.sensitivitet}" }

    fun removeMicrofrontend(microfrontendId: String) =
        newData.removeIf { node ->
            node["microfrontend_id"].asText() == microfrontendId
        }

    fun contentLogMessage() =
        newData.joinToString(prefix = "[", postfix = "]", separator = ",") { it["microfrontend_id"].asText() }

    fun apiResponse(innloggetnivå: Int, manifestMap: Map<String, String>): String = """
        { 
           "microfrontends": ${
        newData
            .filter { Sensitivitet.fromJsonNode(it["sensitivitet"]) <= innloggetnivå }
            .mapNotNull {
                val id = it["microfrontend_id"].asText()
                val url = manifestMap[id]
                if (url == null) {
                    log.error { "Fant ikke manifest for microfrontend med id $id" }
                    null
                } else {
                    mapOf(
                        "microfrontend_id" to id,
                        "url" to manifestMap[id]
                    ).let { contentMap ->
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contentMap)
                    }
                }
            }.jsonArrayString()
    }, 
           "offerStepup": ${newData.any { Sensitivitet.fromJsonNode(it["sensitivitet"]) > innloggetnivå }} 
        }
        """.trimIndent()

    fun newDataJsonB(): PGobject = """{ "microfrontends": ${newData.jsonArrayString()}}""".trimMargin().jsonB()
    fun originalDataJsonB(): PGobject? =
        originalData?.let { """{ "microfrontends": ${it.jsonArrayString()}}""".trimMargin() }?.jsonB()

    private fun Collection<JsonNode>.jsonArrayString(): String = joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    )

    private fun List<String>.jsonArrayString(): String = joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    )

    private fun String.jsonB() = PGobject().apply {
        type = "jsonb"
        value = this@jsonB
    }

    fun getDefinitions(innloggetnivå: Int, manifestMap: Map<String, String>): List<MicrofrontendsDefinition> =
        newData
            .filter { Sensitivitet.fromJsonNode(it["sensitivitet"]) <= innloggetnivå }
            .mapNotNull {
                val id = it["microfrontend_id"].asText()
                val url = manifestMap[id]
                if (url.isNullOrEmpty()) null
                else MicrofrontendsDefinition(id = id, url = url)
            }.also { log.info { "Henter microfrontend definisjoner: $it" } }

    fun offerStepup(innloggetnivå: Int): Boolean =
        newData.any { Sensitivitet.fromJsonNode(it["sensitivitet"]) > innloggetnivå }

    fun ids(): List<String>? = newData.map { it["microfrontend_id"].asText() }

}





