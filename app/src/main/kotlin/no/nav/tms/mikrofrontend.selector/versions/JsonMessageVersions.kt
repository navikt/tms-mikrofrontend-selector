package no.nav.tms.mikrofrontend.selector.versions

import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.sensitivitet

abstract class MessageRequirements(
    vararg eventFields: String
) {
    abstract val action: String
    val commonFields: List<String> = listOf("microfrontend_id", "ident")
    val requiredFields: Array<String> = (commonFields + eventFields).toTypedArray()
}

object JsonMessageVersions {

    object EnableMessage : MessageRequirements("sensitivitet", "@initiated_by") {
        override val action: String = "enable"
    }

    object DisableMessage : MessageRequirements("@initiated_by") {
        override val action: String = "disable"
    }

    /*
    val JsonMessage.sensitivitet: Sensitivitet
        get() = this["sensitivitet"].takeIf { !it.isMissingOrNull() }
            ?.let { name -> Sensitivitet.fromString(name.asText()) }
            ?: this["sikkerhetsnivå"].takeIf { !it.isMissingOrNull() }
                ?.let { nivå -> Sensitivitet.fromSikkerhetsnivå(nivå.asInt()) }
            ?: Sensitivitet.HIGH*/
    val JsonMessage.sensitivitet: Sensitivitet
        get() = this.json.read<String>("sensitivitet")
            ?.let { value -> Sensitivitet.fromString(value) }
            ?: this.json.read<Int>("sikkerhetsnivå")
                ?.let { nivå -> Sensitivitet.fromSikkerhetsnivå(nivå) }
            ?: Sensitivitet.HIGH

    val JsonMessage.initiatedBy: String?
        get() = json.read<String>("@initiated_by")
            ?: json.read<String>("initiated_by")
    fun JsonMessage.traceInfo(sink: String) = mapOf("initiated_by" to (this.initiatedBy ?: ""), "sink" to sink)
}