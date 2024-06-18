package no.nav.tms.mikrofrontend.selector.collector.json

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.*
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import java.lang.NullPointerException
import java.time.LocalDate
import java.time.LocalDateTime


/**
 * Handles jsonpath queries for a string contaning valid json. Wrapperclass
 * JsonPathKtlibrary for https://github.com/codeniko/JsonPathKt
 *
 * @property debugLog set true for verbose output
 * @property jsonNode
 */
class JsonPathInterpreter private constructor(val jsonNode: JsonNode, val debugLog: Boolean = false) {
    val log = KotlinLogging.logger { }
    private fun getAllKeys(jsonNode: JsonNode, keys: MutableSet<String>) {
        when {
            jsonNode.isObject -> {
                jsonNode.fields().forEach { (key, value) ->
                    keys.add(key)
                    getAllKeys(value, keys)
                }
            }

            jsonNode.isArray -> {
                jsonNode.forEach { element ->
                    getAllKeys(element, keys)
                }
            }
        }
    }

    fun hasContent() = jsonNode.let { !it.isEmpty && !it.isNull }

    /**
     * Get the first result of a valid jsonpath, key or nested key in the json.
     * Use [getOrException] for null safe values, or [listOrNull] for lists
     */
    inline fun <reified T : Any> getOrNull(path: String): T? {
        return getWithExceptionHandler<JsonNode>("\$..$path").let { jsonPathResult ->
            when {
                jsonPathResult == null -> {
                    logNotFound(path, jsonNode)
                    null
                }

                jsonPathResult.size() == 0 -> {
                    logNotFound(path, jsonNode)
                    null
                }

                jsonPathResult.size() > 1 -> throw MultipleValuesInJsonPathSearchException(
                    key = path,
                    jsonNode = jsonNode
                )

                else -> {
                    return@let jsonPathResult[0].read<T>("$")
                }
            }
        }
    }

    /**
     * Get the first result of a valid jsonpath, key or nested key in the json
     * and throws an exception if the result is null. Alternatives: [getOrNull]
     * for nullable values, [list] for lists, or [getAllValues] to get all
     * results
     */
    inline fun <reified T : Any> getOrException(key: String): T =
        getOrNull<T>(key) ?: throw JsonPathSearchException(
            key,
            getOrNull<JsonNode>(key), jsonNode, debugLog
        )

    /**
     * Finds all values for a path or key of its position in the json structure
     * returns null if the key is missing. To get the first value of a key
     * [getOrNull] to get a single value
     */
    inline fun <reified T : Any> getAllValues(key: String): List<T>? =
        getWithExceptionHandler<List<T>>("\$..$key").also { jsonPathResult ->
            if (jsonPathResult == null) {
                logNotFound(key, jsonNode)
            }
        }


    inline fun <reified T : Any> getWithExceptionHandler(path: String): T? =
        try {
            jsonNode.read<T>(path)
        } catch (e: Exception) {
            throw JsonPathParseException(e, debugLog, "$path ${jsonNode.toPrettyString()?.redactedMessage()}")
        } catch (nullPointer: NullPointerException) {
            throw JsonPathParseException(
                nullPointer,
                debugLog,
                "$path ${jsonNode.toPrettyString()?.redactedMessage()}"
            )
        }


    /**
     * Get all values of a valid jsonpath, key or nested key in the json
     * structure. For null safe values use [list] For single values use
     * [getOrException]
     */
    inline fun <reified T : Any> listOrNull(path: String): List<T>? =
        getOrNull<List<T>>(path)

    /** TODO: write this */
    inline fun <reified T : Any> getAll(path: String): List<T> =
        getAllValues<T>(path) ?: throw JsonPathSearchException(
            jsonPath = path,
            jsonNode = null,
            originalJson = jsonNode
        )


    /**
     * Get all values of a valid jsonpath, key or nested key in the json
     * structure. For nullable values use [listOrNull] For single values use
     * [getOrNull]
     *
     * @throws JsonPathSearchException
     */
    inline fun <reified T : Any> list(path: String): List<T> =
        getOrNull<List<T>>(path) ?: throw JsonPathSearchException(
            jsonPath = path,
            jsonNode = null,
            originalJson = jsonNode
        )


    fun isNotNull(key: String) = getOrNull<Any>(key) != null
    fun boolean(path: String) = getOrException<Boolean>(path)

    fun booleanOrNull(path: String) = getOrNull<Boolean>(path)
    fun string(path: String) = getOrException<String>(path)
    fun stringOrNull(path: String) = getOrNull<String>(path)
    fun int(path: String) = getOrException<Int>(path)
    fun intOrNull(path: String) = getOrNull<Int>(path)
    fun localDateOrNull(path: String) = getOrNull<LocalDate>(path)
    fun logNotFound(path: String, jsonNode: JsonNode?) {
        log.debug { "Fant ikke '$path' i json: ${jsonNode?.toPrettyString()?.redactedMessage(debugLog) ?: "tom json"}" }
    }

    companion object {
        private val log = KotlinLogging.logger { }
        fun initPathInterpreter(jsonString: String, debugLog: Boolean = false): JsonPathInterpreter? =
            try {
                JsonPath.parse(jsonString).also {
                    if (it == null)
                        log.debug { "Could not parse inputstring to json: ${jsonString.redactedMessage(debugLog)}" }
                }?.let { parsedJson -> JsonPathInterpreter(parsedJson, debugLog) }
            } catch (e: Exception) {
                throw JsonPathParseException(e = e, jsonString = jsonString, debugLog = debugLog)
            }

        fun String.redactedMessage(keepAll: Boolean = false): String =
            replace(Regex("\\d{11}"), "**READCTED**")
                .let {
                    if (keepAll)
                        it
                    else
                        substringOrAll(0..50)
                }

        private fun String.substringOrAll(intRange: IntRange): String =
            if (intRange.last > this.length) this
            else substring(0, intRange.last)

        suspend fun HttpResponse.bodyAsNullOrJsonNode(debugLog: Boolean = false) =
            initPathInterpreter(bodyAsText(), debugLog)
    }

    fun safDokument(dokumentarkivUrlResolver: DokumentarkivUrlResolver) = jsonNode.read<JsonNode>("\$.data.dokumentoversiktSelvbetjening.tema")?.map { it ->
        val datoPath = "\$.journalposter..relevanteDatoer..dato"
        val kodePath = "\$.kode"
        val navnPath = "\$.navn"

        Dokument(
            kode = it.read<String>(kodePath) ?: throw JsonPathSearchException(
                jsonPath = kodePath,
                jsonNode = it,
                originalJson = jsonNode
            ),
            navn  = it.read<String>(navnPath) ?: "ukjent",
            dokumentarkivUrlResolver = dokumentarkivUrlResolver,
            sistEndret = it.read<List<String>>(datoPath)?.also { value -> log.info { "Fra Saf: ${value.joinToString()}" } }
                ?.map { json -> LocalDateTime.parse(json) }
                ?.maxByOrNull { parsedDate -> parsedDate }
                ?: throw JsonPathSearchException(
                    jsonPath =datoPath,
                    jsonNode = it,
                    originalJson = jsonNode
                )
        )
    }

    fun digisosDokument(dokumentarkivUrlResolver: DokumentarkivUrlResolver) = jsonNode.read<JsonNode>("$")?.map {
        val kodePath = "$.kode"
        val datoPath = "$.sistEndret"
        val navnPath = "\$.navn"

        Dokument(
            kode = it.read<String>(kodePath) ?: throw JsonPathSearchException(
                jsonPath = kodePath,
                jsonNode = it,
                originalJson = jsonNode
            ),
            navn  = it.read<String>(navnPath) ?: "ukjent",
            dokumentarkivUrlResolver = dokumentarkivUrlResolver,
            sistEndret = it.read<String>(datoPath)
                ?.let { json -> LocalDateTime.parse(json) }
                ?: throw JsonPathSearchException(
                    jsonPath =datoPath,
                    jsonNode = it,
                    originalJson = jsonNode
                )
        )
    }
}
