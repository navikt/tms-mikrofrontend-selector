package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.microfrontendId
import org.postgresql.util.PGobject

internal class Microfrontends(initialJson: String? = null) {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val originalData: List<JsonNode>? =
        initialJson
            ?.let { objectMapper.readTree(it)["microfrontends"] }
            ?.map {
                if (it.isValueNode) {
                    createNodeAndAddSikkerhetsnivå(it.asText())
                } else {
                    createNode(it["microfrontend_id"].asText(), it["sikkerhetsnivå"].asInt())
                }
            }
            ?.toList()


    private val newData = originalData?.toMutableSet() ?: mutableSetOf()

    companion object {
        fun emptyList(): String = """{ "microfrontends":[] }"""
    }

    fun addMicrofrontend(packet: JsonMessage): Boolean =
        newData
            .find { it["microfrontend_id"].asText() == packet.microfrontendId }
            .let {
                if (it != null) {
                    if (it["sikkerhetsnivå"].asInt() != packet.sikkerhetsnivå) {
                        removeMicrofrontend(packet.microfrontendId)
                        newData.add(createNode(packet.microfrontendId,packet.sikkerhetsnivå))
                    } else {
                        false
                    }
                } else
                    newData.add(createNode(packet.microfrontendId, packet.sikkerhetsnivå))
            }

    fun addMicrofrontend(microfrontendId: String) = newData.add(createNodeAndAddSikkerhetsnivå(microfrontendId))
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

    private fun createNodeAndAddSikkerhetsnivå(microfrontendId: String) = objectMapper.readTree(
        """
         {
         "microfrontend_id": "$microfrontendId",
            "sikkerhetsnivå" : 4
        }
      """.trimMargin()
    )

    private fun createNode(microfrontendId: String, sikkerhetsivå: Int) = objectMapper.readTree(
        """
        {
          "microfrontend_id": "$microfrontendId",
          "sikkerhetsnivå": $sikkerhetsivå
        }
      """.trimMargin()
    )

    private val JsonMessage.sikkerhetsnivå: Int
        get() = get("sikkerhetsnivå").asInt(4)
}

