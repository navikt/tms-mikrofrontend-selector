package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper
import no.nav.tms.mikrofrontend.selector.microfrontendId

private val log = KotlinLogging.logger { }


abstract class KeyRequirements {
    abstract val requiredKeys: List<String>
    abstract val interestedInKeys: List<String>

    fun setRequiredKeys(jsonMessage: JsonMessage) = requiredKeys.forEach { key -> jsonMessage.requireKey(key) }
    fun setInterestedInKeys(jsonMessage: JsonMessage) =
        interestedInKeys.forEach { key -> jsonMessage.interestedIn(key) }
}


object JsonVersions {

    private val requiredKeyBase = listOf("microfrontend_id", "ident")

    object Enabled : KeyRequirements() {
        override val requiredKeys: List<String> = requiredKeyBase
        private val requiredKeysV2 = listOf("sikkerhetsnivå", "@initiated_by")
        val requiredKeysV3 = listOf("sensitivitet", "@initiated_by")
        override val interestedInKeys: List<String> = (requiredKeysV2 + requiredKeysV3).toSet().toList()
    }

    object Disabled : KeyRequirements() {
        override val requiredKeys: List<String> = requiredKeyBase
        override val interestedInKeys: List<String> = listOf("@initiated_by")
    }


    fun currentVersionNode(id: String, sensitivitet: Sensitivitet) = microfrontendMapper.readTree(
        """
         {
            "microfrontend_id": "$id",
            "sensitivitet" : "${sensitivitet.name}"
        }
      """.trimMargin()
    )

    //JsonMessage migration
    fun JsonMessage.applyMigrations() = currentVersionNode(this.microfrontendId, this.sensitivitet)

    val JsonMessage.sensitivitet: Sensitivitet
        get() = this["sensitivitet"].senistivitetOrNull()
            ?: this["sikkerhetsnivå"].sensitivitetFromSikkerhetsnivå()
            ?: Sensitivitet.HIGH


    private fun migrateSikkerhetsnivå(jsonNode: JsonNode): JsonNode =
        currentVersionNode(jsonNode.mikrofrontendId, jsonNode.sensitivitet)

    //JsonNode migration
    private val JsonNode.isSecondVersion
        get() = this["sikkerhetsnivå"] != null

    private val JsonNode.isFirstVersion
        get() = isValueNode


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
        isSecondVersion -> migrateSikkerhetsnivå(this)
        isFirstVersion -> currentVersionNode(asText(), Sensitivitet.HIGH)
        else -> this
    }
}


enum class Sensitivitet(val sikkerhetsnivå: Int) {
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

private val JsonNode.mikrofrontendId: String
    get() = this["microfrontend_id"].asText()

