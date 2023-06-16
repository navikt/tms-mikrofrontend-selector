package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.applyMigrations
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.sensitivitet
import no.nav.tms.mikrofrontend.selector.microfrontendId
import org.postgresql.util.PGobject

private val log = KotlinLogging.logger { }

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
        newData
            .find { it["microfrontend_id"].asText() == packet.microfrontendId }
            ?.let {
                log.info { "oppdaterer eksisterende mikrofrontend med id ${packet.microfrontendId}" }
                val originalSensitivitet = it.sensitivitet
                if (originalSensitivitet == packet.sensitivitet) {
                    false
                } else {
                    log.info { "Endring av sikkerhetsnivå for ${packet.microfrontendId} fra $originalSensitivitet til ${packet.sensitivitet}" }
                    removeMicrofrontend(packet.microfrontendId)
                    newData.add(packet.applyMigrations())
                }
            }
            ?: newData.add(packet.applyMigrations())
                .also { "Legger til ny mikrofrontend med id ${packet.microfrontendId} med sensitivitet ${packet.sensitivitet}" }

    fun removeMicrofrontend(microfrontendId: String) =
        newData.removeIf { node ->
            node["microfrontend_id"].asText() == microfrontendId
        }

    fun apiResponse(innloggetnivå: Int): String = """
        { 
           "microfrontends": ${
        newData
            .filter { Sensitivitet.valueOf(it["sensitivitet"].asText()) <= innloggetnivå }
            .map { it["microfrontend_id"] }
            .jsonArrayString()
    }, 
           "offerStepup": ${newData.any { Sensitivitet.valueOf(it["sensitivitet"].asText()) > innloggetnivå }} 
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

    private fun String.jsonB() = PGobject().apply {
        type = "jsonb"
        value = this@jsonB
    }

}





