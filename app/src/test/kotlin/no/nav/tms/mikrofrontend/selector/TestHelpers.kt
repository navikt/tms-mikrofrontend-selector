package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.MessageRequirements
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet.HIGH

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

object LegacyJsonMessages {

    private fun v1Map(ident: String, microfrontendId: String, messageRequirements: MessageRequirements) = mapOf(
        "@action" to messageRequirements.action,
        "ident" to ident,
        "microfrontend_id" to microfrontendId
    )

    fun v1Message(ident: String, microfrontendId: String, messageRequirements: MessageRequirements) =
        JsonMessage.newMessage(
            v1Map(ident, microfrontendId, messageRequirements)
        ).apply { messageRequirements.addRequiredAndInterestedIn(this) }

    fun enableV2Message(
        ident: String,
        microfrontendId: String,
        initiatedBy: String = "defaultteam",
        sikkerhetsnivå: Int = 4
    ) =
        JsonMessage.newMessage(
            v1Map(ident, microfrontendId, EnableMessage) +
                    mapOf(
                        "initiated_by" to initiatedBy,
                        "sikkerhetsnivå" to sikkerhetsnivå
                    )
        ).apply { EnableMessage.addRequiredAndInterestedIn(this) }

    fun disableV2Message(ident: String, microfrontendId: String, initiatedBy: String) =
        JsonMessage.newMessage(
            v1Map(ident, microfrontendId, JsonMessageVersions.DisableMessage) +
                    mapOf("initiated_by" to initiatedBy)
        ).apply { JsonMessageVersions.DisableMessage.addRequiredAndInterestedIn(this) }
}


fun legacyMessagev2(
    microfrontendId: String,
    ident: String,
    sikkerhetsnivå: Int = 4,
    initiatedBy: String? = "default-team",
) =
    """
    {
      "@action": "enable",
      "ident": "$ident",
      "microfrontend_id": "$microfrontendId",
      "sikkerhetsnivå" : $sikkerhetsnivå
      ${initiatedBy?.let { """ ,"initiated_by": "$initiatedBy" """ } ?: ""}
    }
    """.trimIndent()


fun currentVersionMessage(
    messageRequirements: MessageRequirements = EnableMessage,
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initiatedBy: String = "default-team"
) = JsonMessage.newMessage(
    currentVersionMap(
        messageRequirements = messageRequirements,
        microfrontendId = microfrontendId,
        ident = ident,
        sensitivitet = sensitivitet,
        initiatedBy = initiatedBy
    )
).toJson()

fun currentVersionPacket(
    messageRequirements: MessageRequirements = EnableMessage,
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initatedBy: String = "default-team"
) =
    JsonMessage.newMessage(
        currentVersionMap(messageRequirements, microfrontendId, ident, sensitivitet, initatedBy)
    ).apply {
        messageRequirements.addRequiredAndInterestedIn(this)
    }

fun currentVersionMap(
    messageRequirements: MessageRequirements,
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initiatedBy: String = "default-team"
) = mutableMapOf(
    "@action" to messageRequirements.action,
    "ident" to ident,
    "microfrontend_id" to microfrontendId,
    "@initiated_by" to initiatedBy
).apply {
    if (messageRequirements == EnableMessage)
        this["sensitivitet"] = sensitivitet.stringValue
    println(this)
}


private fun MessageRequirements.addRequiredAndInterestedIn(jsonMessage: JsonMessage) {
    requireCommonKeys(jsonMessage)
    interestedInLegacyKeys(jsonMessage)
    interestedInCurrentVersionKeys(jsonMessage)
}