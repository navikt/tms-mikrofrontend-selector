package no.nav.tms.mikrofrontend.selector.collector.json

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import java.time.LocalDateTime

/**
 * Parses SAF and Digisos document responses into Dokument objects.
 */
object DokumentParser {

    fun parseSafDokument(
        jsonInterpreter: JsonPathInterpreter,
        dokumentarkivUrlResolver: DokumentarkivUrlResolver
    ): List<Dokument>? {
        return jsonInterpreter.jsonNode.read<JsonNode>("\$.data.dokumentoversiktSelvbetjening.tema")?.map { node ->
            val datoPath = "\$.journalposter..relevanteDatoer..dato"
            val kodePath = "\$.kode"
            val navnPath = "\$.navn"

            Dokument(
                kode = node.read<String>(kodePath) ?: throw JsonPathSearchException(
                    jsonPath = kodePath,
                    jsonNode = node,
                    originalJson = jsonInterpreter.jsonNode
                ),
                navn = node.read<String>(navnPath) ?: "ukjent",
                dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                sistEndret = node.read<List<String>>(datoPath)
                    ?.map { json -> LocalDateTime.parse(json) }
                    ?.maxByOrNull { parsedDate -> parsedDate }
                    ?: throw JsonPathSearchException(
                        jsonPath = datoPath,
                        jsonNode = node,
                        originalJson = jsonInterpreter.jsonNode
                    )
            )
        }
    }

    fun parseDigisosDokument(
        jsonInterpreter: JsonPathInterpreter,
        dokumentarkivUrlResolver: DokumentarkivUrlResolver
    ): List<Dokument>? {
        return jsonInterpreter.jsonNode.read<JsonNode>("$")?.map { node ->
            val kodePath = "$.kode"
            val datoPath = "$.sistEndret"
            val navnPath = "\$.navn"

            Dokument(
                kode = node.read<String>(kodePath) ?: throw JsonPathSearchException(
                    jsonPath = kodePath,
                    jsonNode = node,
                    originalJson = jsonInterpreter.jsonNode
                ),
                navn = node.read<String>(navnPath) ?: "ukjent",
                dokumentarkivUrlResolver = dokumentarkivUrlResolver,
                sistEndret = node.read<String>(datoPath)
                    ?.let { json -> LocalDateTime.parse(json) }
                    ?: throw JsonPathSearchException(
                        jsonPath = datoPath,
                        jsonNode = node,
                        originalJson = jsonInterpreter.jsonNode
                    )
            )
        }
    }
}

