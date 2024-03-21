package no.nav.tms.mikrofrontend.selector.collector.json

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.*
import java.lang.NullPointerException
import java.time.LocalDate

/**
 * Handles jsonpath queries for a string contaning valid json
 *
 * @property debugLog set true for verbose output
 * @property parsedJsonPath
 */
class JsonPathInterpreter private constructor(jsonNode: JsonNode, val debugLog: Boolean = false) {
    val log = KotlinLogging.logger { }
    val parsedJsonPath = jsonNode
    private val keysInJsonNode = mutableSetOf<String>().apply { getAllKeys(jsonNode, this) }
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

    fun hasContent() = parsedJsonPath.let { !it.isEmpty && !it.isNull }
    fun isSimplePath(path: String) = path.matches(Regex(KEY_REGEX))

    /**
     * Finds the first value for any valid jsonpath .
     * Use [getFromPathOrException] for nullsafe values
     * Use [getAllValuesForPath] to get all results for the path
     *
     * @param path valid jsonpath, omitting the root element ($.). use the JsonPath.read<T> function for paths contatining the root element
     */
    inline fun <reified T : Any> getFromPath(path: String): T? =
        getWithExceptionHandler<T>("\$.$path")
            .also { if (it == null) logNotFound(path, parsedJsonPath) }

    /**
     * Finds the first value for any valid jsonpath . Throws an exception if the value is not found.
     * Use [getFromPath] for nullable values
     * Use [getAllValuesForPath] to get all results for the path
     * @param path valid jsonpath, omitting the root element ($.). use the JsonPath.read<T> function for paths contatining the root element
     * @throws JsonPathSearchException if the value is not found
     */
    inline fun <reified T : Any> getFromPathOrException(path: String) =
        getWithExceptionHandler<T>(path) ?: throw JsonPathSearchException(path, parsedJsonPath, debugLog)

    /**
     * Finds the first value of a key regardless of its position in the jsonstructure returns null if the key is missing.
     * Use [getFromKeyOrException] for nullsafe values.
     * Supports single or nested keys, for more complex paths use [getFromPath]
     * Returns the first element of a key use [getAllValuesForKey] to get all results
     *
     * @param key any single or nested key in the jsonstructure
     */
    inline fun <reified T : Any> getFromKey(key: String): T? {
        return getWithExceptionHandler<JsonNode>("\$..$key").let { jsonPathResult ->
            when {
                jsonPathResult == null -> {
                    logNotFound(key, parsedJsonPath)
                    null
                }

                jsonPathResult.size() == 0 -> {
                    logNotFound(key, parsedJsonPath)
                    null
                }

                jsonPathResult.size() > 1 -> throw MultipleValuesInJsonPathSearchException(
                    key = key,
                    jsonNode = parsedJsonPath
                )

                else -> {
                    return@let jsonPathResult[0].read<T>("$")
                }
            }
        }
    }

    /**
     * Finds a given key regardless of its position in the jsonstructure returns null if the key is missing.
     * Use [getFromKeyOrException] for nullable values.
     * Supports single or nested keys, for more complex paths use [getFromPathOrException]
     * Returns the first element of a key use [getAllValuesForKey] to get all results
     *
     * @param key any single or nested key in the jsonstructure
     * @return value of key
     */
    inline fun <reified T : Any> getFromKeyOrException(key: String) =
        getFromKey<T>(key) ?: throw JsonPathSearchException(key, parsedJsonPath, debugLog)

    /**
     * Finds a given key regardless of its position in the jsonstructure returns null if the key is missing.
     * Supports single or nested keys, for more complex paths use [getAllValuesForPath]
     * Returns all found elements of a key [getFromKey] to get a single value
     *
     * @param key any single or nested key in the jsonstructure
     */
    inline fun <reified T : Any> getAllValuesForKey(key: String): List<T>? =
        getWithExceptionHandler<JsonNode>("\$..$key").let { jsonPathResult ->
            if (jsonPathResult == null) {
                logNotFound(key, parsedJsonPath)
            }
            jsonPathResult?.read<List<T>>("$")
        }

    /**
     * Finds all values for any valid jsonpath. Returns null if there are no results
     * Use [getAllValuesForPath] to get all results for the path
     * @param path valid jsonpath omitting the root element ($.). use the JsonPath.read function for paths contatining the root element
     */

    inline fun <reified T : Any> getAllValuesForPath(path: String): List<T>? =
        getWithExceptionHandler<JsonNode>("\$.$path").let { jsonPathResult ->
            if (jsonPathResult == null) {
                logNotFound(path, parsedJsonPath)
            }
            jsonPathResult?.read<List<T>>("$")
        }
    inline fun <reified T : Any> getWithExceptionHandler(path: String): T? =
        try {
            parsedJsonPath.read<T>(path)
        } catch (e: Exception) {
            throw JsonPathParseException(e, debugLog, "$path ${parsedJsonPath.toPrettyString()?.redactedMessage()}")
        } catch (nullpointer: NullPointerException) {
            throw JsonPathParseException(
                nullpointer,
                debugLog,
                "$path ${parsedJsonPath.toPrettyString()?.redactedMessage()}"
            )
        }

    /**
     * Get the first value of a valid jsonpath, key or nested key in the jsonstructure.
     * For nullsafe values use [getOrException]
     */
    inline fun <reified T : Any> getOrNull(path: String): T? =
        if (isSimplePath(path))
            getFromKey<T>(path)
        else {
            getFromPath(path)
        }

    /**
     * Get the first value of a valid jsonpath, key or nested key in the jsonstructure.
     * For nullablevalues use [getOrNull]
     * For all values use [list]
     */
    private inline fun <reified T : Any> getOrException(path: String): T =
        if (isSimplePath(path))
            getFromKeyOrException(path)
        else {
            getFromPathOrException(path)
        }

    /**
     * Get all values of a valid jsonpath, key or nested key in the jsonstructure.
     * For nullsafe values use [list]
     * For single values use [getOrException]
     */
    inline fun <reified T : Any> listOrNull(path: String): List<T>? =
        if (isSimplePath(path))
            getAllValuesForKey<T>(path)
        else {
            getAllValuesForPath<T>(path)
        }

    /**
     * Get all values of a valid jsonpath, key or nested key in the jsonstructure.
     * For nullable values use [listOrNull]
     * For single values use [getOrNull]
     * @throws JsonPathSearchException
     */
    inline fun <reified T : Any> list(path: String): List<T>? =
        if (isSimplePath(path))
            getAllValuesForKey<T>(path) ?: throw JsonPathSearchException(jsonPath = path, jsonNode = parsedJsonPath)
        else {
            getAllValuesForPath<T>(path) ?: throw JsonPathSearchException(jsonPath = path, jsonNode = parsedJsonPath)
        }

    fun isNotNull(key: String) = getOrNull<Any>(key) != null
    fun boolean(path: String) = getOrException<Boolean>(path)

    fun booleanOrNull(path: String) = getOrNull<Boolean>(path)
    fun string(path: String) = getOrException<String>(path)

    fun stringOrNull(path: String) = getOrNull<String>(path)
    fun int(path: String) = getOrException<Int>(path)
    fun intOrNull(path: String) = getOrNull<Int>(path)
    fun localdateOrNull(path: String) = getOrNull<LocalDate>(path)
    fun logNotFound(path: String, jsonNode: JsonNode?) {
        log.debug { "Fant ikke '$path' i json: ${jsonNode?.toPrettyString()?.redactedMessage(debugLog) ?: "tom json"}" }
    }
    fun getSuggestions(path: String) {
        val keysInPath = "^[0-9a-zA-ZæøåÆØÅ_\\-]+".toRegex().findAll(path).map { it.value }.toList()

        if (keysInPath.size == 1) {
            if (keysInJsonNode.contains(keysInPath.first()))
                print("YEI! key exists, why do you want suggesstions?")
            else print(findPossibleMatches(keysInPath.first()))
        }

    }

    private fun findPossibleMatches(keyFromPath: String): String {
        val firstTwoLetters = keyFromPath.substring(0, 1)
        val lastTwoLetters = keyFromPath.substring(keyFromPath.length - 4, keyFromPath.length - 1)

        val existingKeysPrefix = keysInJsonNode.map { it.substring(0, 2) }.filter {
            it.startsWith(firstTwoLetters)
        }
        val existingKeysPostfix = keysInJsonNode.map { it.substring(it.length - 4, it.length - 1) }.filter {
            it.endsWith(lastTwoLetters)
        }

        return (existingKeysPostfix + existingKeysPrefix).joinToString()
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
        private const val KEY_REGEX = "^[0-9a-zA-ZæøåÆØÅ_\\-]+(\\.[0-9a-zA-Z_æøåÆØÅ\\-]+)*"

    }
}