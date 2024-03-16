package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.mikrofrontend.selector.collector.NullOrJsonNode.Companion.logsafeMessage

class NullOrJsonNode private constructor(jsonString: String) {
    val parsedJsonPath = try {
        JsonPath.parse(jsonString)
    } catch (e: Exception) {
        throw JsonPathParseException(e, jsonString)
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

                jsonPathResult.size() > 1 -> throw JsonPathParseException(jsonString = "More than one value found for key, use getAllValuesForKey instead")
                else -> {
                    return@let jsonPathResult[0].read<T>("$")
                }
            }
        }

    inline fun <reified T : Any> getAllValuesForKey(key: String): List<T>? =
        getWithExceptionHandler<JsonNode>("\$..$key").let { jsonPathResult ->
            if (jsonPathResult == null) {
                logNotFound(key, parsedJsonPath)
            }
            jsonPathResult?.read<List<T>>("$")
        }

    inline fun <reified T : Any> getWithExceptionHandler(path: String): T? =
        try {
            parsedJsonPath?.read<T>(path)
        } catch (e: Exception) {
            throw JsonPathParseException(e, "$path ${parsedJsonPath?.toPrettyString()?.logsafeMessage()}")
        }

    companion object {
        val log = KotlinLogging.logger { }
        fun initObjectMapper(jsonString: String) = NullOrJsonNode(jsonString).let {
            if (it.parsedJsonPath == null) {
                log.warn { "Could not parse inputstring to json: ${jsonString.logsafeMessage()}" }
                null
            } else it
        }

        fun String.logsafeMessage(): String = substringOrAll(0..50)
            .replace(Regex("\\d{11}"), "**READCTED**")

        private fun String.substringOrAll(intRange: IntRange): String =
            if (intRange.last > this.length) this
            else substring(0, intRange.last)

        fun logNotFound(path: String, jsonNode: JsonNode?) {
            log.warn { "Fant ikke '$path' i json: ${jsonNode?.toPrettyString()?.logsafeMessage() ?: "tom json"}" }
        }
    }
}

class JsonPathParseException(e: Exception? = null, jsonString: String) : IllegalArgumentException() {
    private var info: String

    init {
        info =
            "Failed to parse jsonString from string ${jsonString.logsafeMessage()}: ${e?.let { e.message ?: e::class.simpleName } ?: ""}"
    }
}


