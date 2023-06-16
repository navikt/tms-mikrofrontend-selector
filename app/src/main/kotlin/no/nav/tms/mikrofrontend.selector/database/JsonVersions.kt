package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper
import no.nav.tms.mikrofrontend.selector.microfrontendId

private val log = KotlinLogging.logger { }


abstract class KeyRequirements {
    abstract val action: String
    val commonKeys: List<String> = listOf("microfrontend_id", "ident")
    abstract val olderVersionKeys: List<String>
    abstract val currentVersionKeys: List<String>
    fun requireCommonKeys(jsonMessage: JsonMessage) = commonKeys.forEach { key -> jsonMessage.requireKey(key) }
    fun interestedInLegacyKeys(jsonMessage: JsonMessage) =
        olderVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }
    fun interestedInCurrentVersionKeys(jsonMessage: JsonMessage) =
        currentVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }
}


object JsonVersions {
    private val JsonNode.isSecondVersion
        get() = this["sikkerhetsnivå"] != null
    private val JsonNode.isFirstVersion
        get() = isValueNode

    object EnableKeys : KeyRequirements() {
        override val action: String = "enable"
        private val requiredKeysV2 = listOf("sikkerhetsnivå", "initiated_by")
        override val olderVersionKeys = requiredKeysV2
        override val currentVersionKeys = listOf("sensitivitet", "@initiated_by")
    }

    object DisableKeys : KeyRequirements() {
        override val action: String = "disable"
        private val requiredKeysV2 = listOf("initiated_by")
        override val olderVersionKeys = requiredKeysV2
        override val currentVersionKeys: List<String> = listOf("@initiated_by")
    }

    private fun currentVersionNode(id: String, sensitivitet: Sensitivitet) = microfrontendMapper.readTree(
        """
         {
            "microfrontend_id": "$id",
            "sensitivitet" : "${sensitivitet.name}"
        }
      """.trimMargin()
    )

    fun JsonMessage.applyMigrations(): JsonNode = currentVersionNode(this.microfrontendId, this.sensitivitet)
    val JsonMessage.sensitivitet: Sensitivitet
        get() = this["sensitivitet"].senistivitetOrNull()
            ?: this["sikkerhetsnivå"].sensitivitetFromSikkerhetsnivå()
            ?: Sensitivitet.HIGH

    val JsonNode.sensitivitet: Sensitivitet
        get() =
            this["sensitivitet"].senistivitetOrNull()
                ?: this["sikkerhetsnivå"].sensitivitetFromSikkerhetsnivå()
                ?: Sensitivitet.HIGH

    private fun JsonNode?.sensitivitetFromSikkerhetsnivå() =
        this
            ?.takeIf { !isMissingOrNull() }
            ?.let { nivå -> Sensitivitet.resolve(nivå.asInt()) }

    private fun JsonNode?.senistivitetOrNull() = this
        ?.takeIf { !isMissingOrNull() }?.textValue()?.let { name -> Sensitivitet.valueOf(name) }

    fun JsonNode.applyMigrations(): JsonNode = when {
        isSecondVersion -> currentVersionNode(this["microfrontend_id"].asText(), this.sensitivitet)
        isFirstVersion -> currentVersionNode(asText(), Sensitivitet.HIGH)
        else -> this
    }
}


enum class Sensitivitet(private val sikkerhetsnivå: Int) {
    HIGH(4), SUBSTANTIAL(3);

    operator fun compareTo(innloggetnivå: Int): Int =
        sikkerhetsnivå - resolve(innloggetnivå).sikkerhetsnivå

    companion object {
        fun resolve(sikkerhetsnivå: Int?) = when (sikkerhetsnivå) {
            null -> HIGH
            4 -> HIGH
            3 -> SUBSTANTIAL
            else -> {
                log.error { "$sikkerhetsnivå har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
                HIGH
            }
        }
    }
}

val JsonMessage.initiatedBy: String?
    get() =
        get("@initiated_by")
            .takeIf { !it.isMissingOrNull() }?.asText()
            ?: get("initiated_by").asText()

