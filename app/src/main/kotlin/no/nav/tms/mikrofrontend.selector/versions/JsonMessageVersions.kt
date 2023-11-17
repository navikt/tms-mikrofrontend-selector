package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.metrics.MessageVersionCounter
import no.nav.tms.mikrofrontend.selector.microfrontendId

abstract class MessageRequirements(private val messageVersionCounter: MessageVersionCounter) {

    private val log = KotlinLogging.logger { }
    abstract val action: String
    val commonKeys: List<String> = listOf("microfrontend_id", "ident")
    abstract val requiredKeysV2: List<String>
    abstract val olderVersionKeys: List<String>
    abstract val latestVersionKeys: List<String>

    fun countVersion(jsonMessage: JsonMessage) {
        val keysInMessage =
            (commonKeys + olderVersionKeys + latestVersionKeys).filter { key ->
                !jsonMessage[key].isMissingNode
            }
        when {
            keysInMessage.containsAll(latestVersionKeys) -> messageVersionCounter.countMessageVersion(
                "V3",
                jsonMessage.microfrontendId,
                jsonMessage["@initiated_by"].asText()
            )

            keysInMessage.containsAll(requiredKeysV2) -> messageVersionCounter.countMessageVersion(
                "V2",
                jsonMessage.microfrontendId,
                jsonMessage["initiated_by"].asText()
            )

            (olderVersionKeys + latestVersionKeys).none { keysInMessage.contains(it) } ->
                messageVersionCounter.countMessageVersion("V1", jsonMessage.microfrontendId)

            else -> {
                messageVersionCounter.countMessageVersion(microfrontendId = jsonMessage.microfrontendId)
                log.info { "mottok enablemelding med ukjent kombinasjon av nøkler: ${keysInMessage.joinToString(",")}" }
            }
        }
    }

    fun requireCommonKeys(jsonMessage: JsonMessage) = commonKeys.forEach { key -> jsonMessage.requireKey(key) }
    fun interestedInLegacyKeys(jsonMessage: JsonMessage) =
        olderVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }

    fun interestedInCurrentVersionKeys(jsonMessage: JsonMessage) =
        latestVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }

}

object JsonMessageVersions {
    val messageVersionCounter = MessageVersionCounter()
    object EnableMessage : MessageRequirements(messageVersionCounter) {
        override val action: String = "enable"
        override val requiredKeysV2 = listOf("sikkerhetsnivå", "initiated_by")
        override val olderVersionKeys = requiredKeysV2
        override val latestVersionKeys = listOf("sensitivitet", "@initiated_by")
    }

    object DisableMessage : MessageRequirements(messageVersionCounter) {
        override val action: String = "disable"
        override val requiredKeysV2 = listOf("initiated_by")
        override val olderVersionKeys = requiredKeysV2
        override val latestVersionKeys: List<String> = listOf("@initiated_by")
    }

    fun JsonMessage.toDbNode(): JsonNode =
        DatabaseJsonVersions.currentVersionNode(this.microfrontendId, this.sensitivitet)

    val JsonMessage.sensitivitet: Sensitivitet
        get() = this["sensitivitet"].takeIf { !it.isMissingOrNull() }
            ?.let { name -> Sensitivitet.fromString(name.asText()) }
            ?: this["sikkerhetsnivå"].takeIf { !it.isMissingOrNull() }
                ?.let { nivå -> Sensitivitet.fromSikkerhetsnivå(nivå.asInt()) }
            ?: Sensitivitet.HIGH

    val JsonMessage.initiatedBy: String?
        get() = this["@initiated_by"].takeIf { !it.isMissingOrNull() }?.asText()
            ?: this["initiated_by"].takeIf { !it.isMissingOrNull() }?.asText()

    fun JsonMessage.traceInfo(sink:String) = mapOf("initiated_by" to (this.initiatedBy ?: ""),"sink" to sink)
}