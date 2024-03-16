package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.*
import no.nav.tms.mikrofrontend.selector.collector.NullOrJsonNode.Companion.redactedMessage

class NullOrJsonNode private constructor(jsonString: String, val debugLog: Boolean = false) {
    val log = KotlinLogging.logger { }
    val parsedJsonPath = try {
        JsonPath.parse(jsonString).also {
            if (it == null)
                log.warn { "Could not parse inputstring to json: ${jsonString.redactedMessage(debugLog)}" }
        }
    } catch (e: Exception) {
        throw JsonPathParseException(e = e, jsonString = jsonString, debugLog = debugLog)
    }

    inline fun <reified T : Any> getFromPath(path: String): T? =
        getWithExceptionHandler<T>("\$.$path")
            .also { if (it == null) logNotFound(path, parsedJsonPath) }

    inline fun <reified T : Any> getFromKey(key: String): T? =
        getWithExceptionHandler<JsonNode>("\$..$key").let { jsonPathResult ->
            when {
                jsonPathResult == null -> {
                    logNotFound(key, parsedJsonPath)
                    null
                }

                jsonPathResult.size() == 0 -> {
                    logNotFound(key, parsedJsonPath)
                    null
                }

                jsonPathResult.size() > 1 -> throw MultipleValuesInJsonPathSearchException(key = key, jsonNode = parsedJsonPath)
                else -> {
                    return@let jsonPathResult[0].read<T>("$")
                }
            }
        }

    inline fun <reified T : Any> getFromKeyOrException(key: String, logFull: Boolean = false) =
        getFromKey<T>(key) ?: throw JsonPathSearchException(key,parsedJsonPath,debugLog)

    inline fun <reified T : Any> getAllValuesForKey(key: String): List<T>? =
        getWithExceptionHandler<JsonNode>("\$..$key").let { jsonPathResult ->
            if (jsonPathResult == null) {
                logNotFound(key, parsedJsonPath)
            }
            jsonPathResult?.read<List<T>>("$")
        }

    inline fun <reified T : Any> getAllValuesForPath(path: String): List<T>? =
        getWithExceptionHandler<JsonNode>("\$.$path").let { jsonPathResult ->
            if (jsonPathResult == null) {
                logNotFound(path, parsedJsonPath)
            }
            jsonPathResult?.read<List<T>>("$")
        }

    inline fun <reified T : Any> getWithExceptionHandler(path: String): T? =
        try {
            parsedJsonPath?.read<T>(path)
        } catch (e: Exception) {
            throw JsonPathParseException(e, debugLog, "$path ${parsedJsonPath?.toPrettyString()?.redactedMessage()}")
        }

    fun logNotFound(path: String, jsonNode: JsonNode?) {
        log.warn { "Fant ikke '$path' i json: ${jsonNode?.toPrettyString()?.redactedMessage(debugLog) ?: "tom json"}" }
    }

    companion object {
        fun initObjectMapper(jsonString: String, debugLog: Boolean = false) = NullOrJsonNode(jsonString,debugLog).let {
            if (it.parsedJsonPath == null) {
                null
            } else it
        }

        fun String.redactedMessage(keepAll: Boolean = false): String =
            replace(Regex("\\d{11}"), "**READCTED**")
                .let {
                    if (keepAll)
                        it
                    else
                        substringOrAll(0..20)
                }

        private fun String.substringOrAll(intRange: IntRange): String =
            if (intRange.last > this.length) this
            else substring(0, intRange.last)

        suspend fun HttpResponse.bodyAsJsonNode(debugLog: Boolean=false) = initObjectMapper(bodyAsText(),debugLog)

    }
}

class JsonPathParseException(e: Exception? = null, debugLog: Boolean = false, jsonString: String) :
    IllegalArgumentException() {
    private var info: String

    init {
        info =
            "Failed to parse jsonString from string ${jsonString.redactedMessage(debugLog)}: ${e?.let { e.message ?: e::class.simpleName } ?: ""}"
    }
}

class JsonPathSearchException(jsonPath: String, jsonNode: JsonNode?, debugLog: Boolean = false) :
    IllegalArgumentException("Failed to find $jsonPath in ${jsonNode?.toPrettyString()?.redactedMessage(debugLog)}" ) {
}

class MultipleValuesInJsonPathSearchException(key: String, jsonNode: JsonNode?, debugLog: Boolean = false) :
    IllegalArgumentException() {
    private var info: String

    init {
        info =
            "More than one value found for key $key ${jsonNode?.toPrettyString()?.redactedMessage(debugLog)}, use getAllValuesForKey instead"
    }
}





