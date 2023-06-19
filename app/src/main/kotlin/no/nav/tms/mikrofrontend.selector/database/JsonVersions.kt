package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper
import no.nav.tms.mikrofrontend.selector.metrics.MessageVersionCounter
import no.nav.tms.mikrofrontend.selector.microfrontendId

private val log = KotlinLogging.logger { }


abstract class MessageRequirements(private val messageVersionCounter: MessageVersionCounter) {
    abstract val action: String
    val commonKeys: List<String> = listOf("microfrontend_id", "ident")
    abstract val requiredKeysV2: List<String>
    abstract val olderVersionKeys: List<String>
    abstract val currentVersionKeys: List<String>

    fun countVersion(jsonMessage: JsonMessage) {
        /*val keysInMessage =
            (commonKeys + olderVersionKeys + currentVersionKeys).filter { !jsonMessage["it"].isMissingNode }
        when {
            keysInMessage.containsAll(currentVersionKeys) -> messageVersionCounter.countMessageVersion(
                "3",
                jsonMessage.microfrontendId,
                jsonMessage["@initiated_by"].asText()
            )

            keysInMessage.containsAll(requiredKeysV2) -> messageVersionCounter.countMessageVersion(
                "2",
                jsonMessage.microfrontendId,
                jsonMessage["@initiated_by"].asText()
            )

            (olderVersionKeys + currentVersionKeys).none { keysInMessage.contains(it) } ->
                messageVersionCounter.countMessageVersion("1", jsonMessage.microfrontendId)

            else -> {
                messageVersionCounter.countMessageVersion(microfrontendId = jsonMessage.microfrontendId)
                log.info { "mottok enablemelding med ukjent kombinasjon av nøkler: ${keysInMessage.joinToString(",")}" }
            }
        }*/
    }
    fun requireCommonKeys(jsonMessage: JsonMessage) = commonKeys.forEach { key -> jsonMessage.requireKey(key) }
    fun interestedInLegacyKeys(jsonMessage: JsonMessage) =
        olderVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }

    fun interestedInCurrentVersionKeys(jsonMessage: JsonMessage) =
        currentVersionKeys.forEach { key -> jsonMessage.interestedIn(key) }

}


object JsonVersions {
    private val messageVersionCounter = MessageVersionCounter()
    private val JsonNode.isSecondVersion
        get() = this["sikkerhetsnivå"] != null

    private val JsonNode.isFirstVersion
        get() = isValueNode

    object EnableMessage : MessageRequirements(messageVersionCounter) {
        override val action: String = "enable"
        override val requiredKeysV2 = listOf("sikkerhetsnivå", "initiated_by")
        override val olderVersionKeys = requiredKeysV2
        override val currentVersionKeys = listOf("sensitivitet", "@initiated_by")


    }

    object DisableMessage : MessageRequirements(messageVersionCounter) {
        override val action: String = "disable"
        override val requiredKeysV2 = listOf("initiated_by")
        override val olderVersionKeys = requiredKeysV2
        override val currentVersionKeys: List<String> = listOf("@initiated_by")
    }

    private fun currentVersionDbNode(id: String, sensitivitet: Sensitivitet) = microfrontendMapper.readTree(
        """
         {
            "microfrontend_id": "$id",
            "sensitivitet" : "${sensitivitet.name}"
        }
      """.trimMargin()
    )

    fun JsonMessage.toDbNode(): JsonNode = currentVersionDbNode(this.microfrontendId, this.sensitivitet)
    val JsonMessage.sensitivitet: Sensitivitet
        get() = this["sensitivitet"].senistivitetOrNull()
            ?: this["sikkerhetsnivå"].sensitivitetFromSikkerhetsnivå()
            ?: Sensitivitet.HIGH

    val JsonMessage.initiatedBy: String?
        get() =
            this["@initiated_by"]
                .takeIf { !it.isMissingOrNull() }?.asText()
                ?: this["initiated_by"].asText()

    val JsonNode.sensitivitet: Sensitivitet
        get() =
            this["sensitivitet"].senistivitetOrNull()
                ?: this["sikkerhetsnivå"].sensitivitetFromSikkerhetsnivå()
                ?: Sensitivitet.HIGH

    private fun JsonNode?.sensitivitetFromSikkerhetsnivå() = this
        ?.takeIf { !isMissingOrNull() }
        ?.let { nivå -> Sensitivitet.resolve(nivå.asInt()) }

    private fun JsonNode?.senistivitetOrNull() = this
        ?.takeIf { !isMissingOrNull() }
        ?.textValue()
        ?.let { name -> Sensitivitet.valueOf(name) }

    fun JsonNode.applyMigrations(): JsonNode = when {
        isSecondVersion -> currentVersionDbNode(this["microfrontend_id"].asText(), this.sensitivitet)
        isFirstVersion -> currentVersionDbNode(asText(), Sensitivitet.HIGH)
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
