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

class JsonPathSearchException(jsonPath: String, jsonNode: JsonNode?, debugLog: Boolean = false) :
    IllegalArgumentException("Failed to find $jsonPath in ${jsonNode?.toPrettyString()?.redactedMessage(debugLog)}") {
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