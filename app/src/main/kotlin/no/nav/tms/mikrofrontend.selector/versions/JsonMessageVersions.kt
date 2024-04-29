package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.microfrontendId

abstract class MessageRequirements {

    abstract val action: String
    val commonKeys: List<String> = listOf("microfrontend_id", "ident")
    abstract val latestVersionKeys: List<String>

    fun requireCommonKeys(jsonMessage: JsonMessage) = commonKeys.forEach { key -> jsonMessage.requireKey(key) }

    fun interestedInCurrentVersionKeys(jsonMessage: JsonMessage) =
        latestVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }

}

object JsonMessageVersions {

    object EnableMessage : MessageRequirements() {
        override val action: String = "enable"
        override val latestVersionKeys = listOf("sensitivitet", "@initiated_by")
    }

    object DisableMessage : MessageRequirements() {
        override val action: String = "disable"
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

    fun JsonMessage.traceInfo(sink: String) = mapOf("initiated_by" to (this.initiatedBy ?: ""), "sink" to sink)
}