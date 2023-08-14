package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.applyMigrations
import no.nav.tms.mikrofrontend.selector.versions.DatabaseJsonVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.toDbNode
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import org.postgresql.util.PGobject

private val log = KotlinLogging.logger { }
private val objectMapper = jacksonObjectMapper()

internal class Microfrontends(initialJson: String? = null) {

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

    fun apiResponseV1(innloggetnivå: Int): String = """
        { 
           "microfrontends": ${
        newData
            .filter { Sensitivitet.fromJsonNode(it["sensitivitet"]) <= innloggetnivå }
            .map { it["microfrontend_id"] }
            .jsonArrayString()
    }, 
           "offerStepup": ${newData.any { Sensitivitet.fromJsonNode(it["sensitivitet"]) > innloggetnivå }} 
        }
        """.trimIndent()

    fun apiResponseV2(innloggetnivå: Int, manifestMap: Map<String, String>): String = """
        { 
           "microfrontends": ${
        newData
            .filter { Sensitivitet.fromJsonNode(it["sensitivitet"]) <= innloggetnivå }
            .map {
                mapOf(
                    "microfrontend_id" to it["microfrontend_id"],
                    "url" to manifestMap[""]
                ).let { contentMap ->
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contentMap)
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

}





