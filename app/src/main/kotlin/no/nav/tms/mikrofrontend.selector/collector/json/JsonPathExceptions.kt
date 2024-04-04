package no.nav.tms.mikrofrontend.selector.collector.json

import com.fasterxml.jackson.databind.JsonNode
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.redactedMessage


class JsonPathParseException(e: Exception? = null, debugLog: Boolean = false, jsonString: String) :
    IllegalArgumentException() {
    private var info: String

    init {
        info =
            "Failed to parse jsonString from string ${jsonString.redactedMessage(debugLog)}: ${e?.let { e.message ?: e::class.simpleName } ?: ""}"
    }
}

class JsonPathSearchException(
    jsonPath: String,
    jsonNode: JsonNode?,
    originalJson: JsonNode,
    debugLog: Boolean = false
) :
    IllegalArgumentException(createErrorMessage(jsonPath, jsonNode, originalJson, debugLog )) {

    companion object {
        private fun createErrorMessage(
            jsonPath: String,
            jsonNode: JsonNode?,
            originalJsonNode: JsonNode,
            debugLog: Boolean
        ) = when {
                jsonNode == null -> "Failed to find $jsonPath in ${originalJsonNode.toPrettyString()?.redactedMessage(debugLog)}. Possible keys are ${originalJsonNode.keys()}"
                jsonNode.isObject -> """$jsonPath can not be converted to simple value because it is an object in ${
                    jsonNode.toPrettyString().redactedMessage(keepAll = true)
                } . Possible keys are ${jsonNode.keys()}""".trimIndent()
                jsonNode.isArray -> """$jsonPath returns an Array from ${jsonNode.toPrettyString().redactedMessage()}. Possible keys are ${jsonNode.keys()} """.trimIndent()
            else -> "Failed to find $jsonPath in ${jsonNode.toPrettyString().redactedMessage()}. Possible keys are ${originalJsonNode.keys()}".trimIndent()
        }

    }
}


class MultipleValuesInJsonPathSearchException(key: String, jsonNode: JsonNode?, debugLog: Boolean = false) :
    IllegalArgumentException() {
    private var info: String

    init {
        info =
            "More than one value found for key $key ${
                jsonNode?.toPrettyString()?.redactedMessage(debugLog)
            }, use getAllValuesForKey instead"
    }
}


private fun JsonNode?.keys(): String = mutableSetOf<String>().apply {
    if (this@keys != null)
        getAllKeys(this@keys, this@apply)
}.joinToString()

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
