package no.nav.tms.mikrofrontend.selector.versions

import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance

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

    val JsonMessage.levelOfAssurance: LevelOfAssurance
        get() = this.json.read<String>("sensitivitet")
            ?.let { value -> LevelOfAssuranceResolver.fromString(value) }
            ?: this.json.read<Int>("sikkerhetsnivå")
                ?.let { nivå -> LevelOfAssuranceResolver.fromSikkerhetsnivå(nivå) }
            ?: LevelOfAssurance.HIGH

    val JsonMessage.initiatedBy: String?
        get() = json.read<String>("@initiated_by")
            ?: json.read<String>("initiated_by")
    fun JsonMessage.traceInfo(sink: String) = mapOf("initiated_by" to (this.initiatedBy ?: ""), "sink" to sink)
}