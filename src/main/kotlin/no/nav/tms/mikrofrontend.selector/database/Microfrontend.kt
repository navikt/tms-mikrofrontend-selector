package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.microfrontendId
import org.postgresql.util.PGobject

private val log = KotlinLogging.logger { }

internal class Microfrontends(initialJson: String? = null) {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val originalData: List<JsonNode>? =
        initialJson
            ?.let { objectMapper.readTree(it)["microfrontends"] }
            ?.map {
                if (it.isValueNode) {
                    log.info { "Konverterer mikrofrontend-entry fra gammelt til nytt format; ${it.asText()}" }
                    createNodeAndAddSikkerhetsnivå(it.asText())
                } else {
                    log.info { "Leser mikrofrontend-entry på nytt format d d" }
                    createNode(it["microfrontend_id"].asText(), it["sikkerhetsnivå"].asInt())
                }
            }
            ?.toList()


    private val newData = originalData?.toMutableSet() ?: mutableSetOf()

    companion object {
        fun emptyApiResponse(): String = """{ "microfrontends":[], "offerStepup": false }"""
    }

    fun addMicrofrontend(packet: JsonMessage): Boolean =
        newData
            .find { it["microfrontend_id"].asText() == packet.microfrontendId }
            ?.let {
                log.info { "oppdaterer eksisterende mikrofrontend med id ${packet.microfrontendId}" }
                val currentSikkerhetsnivå = it["sikkerhetsnivå"].asInt()
                if (currentSikkerhetsnivå == packet.sikkerhetsnivå) {
                    false
                } else {
                    log.info { "Endring av sikkerhetsnivå for ${packet.microfrontendId} fra $currentSikkerhetsnivå til ${packet.sikkerhetsnivå}" }
                    removeMicrofrontend(packet.microfrontendId)
                    newData.add(createNode(packet.microfrontendId, packet.sikkerhetsnivå))
                }
            }
            ?: newData.add(createNode(packet.microfrontendId, packet.sikkerhetsnivå))
                .also { "Legger til ny mikrofrontend med id ${packet.microfrontendId} med sikkerhetsnivå ${packet.sikkerhetsnivå}" }

    fun removeMicrofrontend(microfrontendId: String) =
        newData.removeIf { node ->
            node["microfrontend_id"].asText() == microfrontendId
        }

    fun apiResponse(innloggetnivå: Int): String = """
        { 
           "microfrontends": ${
        newData
            .filter { it["sikkerhetsnivå"].asInt() <= innloggetnivå }
            .map { it["microfrontend_id"] }
            .jsonArrayString()
    }, 
           "offerStepup": ${newData.any { it["sikkerhetsnivå"].asInt() > innloggetnivå }} 
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

    private fun createNodeAndAddSikkerhetsnivå(microfrontendId: String) = objectMapper.readTree(
        """
         {
         "microfrontend_id": "$microfrontendId",
            "sikkerhetsnivå" : 4
        }
      """.trimMargin()
    )

    private fun createNode(microfrontendId: String, sikkerhetsnivå: Int) = objectMapper.readTree(
        """
        {
          "microfrontend_id": "$microfrontendId",
          "sikkerhetsnivå": $sikkerhetsnivå
        }
      """.trimMargin()
    )

    private fun microfrontendApiList(innloggetnivå: Int) {
        newData
            .filter { it["sikkerhetsnivå"].asInt() <= innloggetnivå }
            .map { it["microfrontend_id"] }
            .jsonArrayString()

    }
}

private val JsonMessage.sikkerhetsnivå: Int
    get() = get("sikkerhetsnivå").asInt(4)





