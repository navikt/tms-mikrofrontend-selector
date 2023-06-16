import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.database.JsonVersions
import no.nav.tms.mikrofrontend.selector.database.MessageRequirements
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.HIGH

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
            v1Map(ident,microfrontendId,messageRequirements)
        ).apply { messageRequirements.addRequiredAndInterestedIn(this) }

    fun enableV2Message(
        ident: String,
        microfrontendId: String,
        initiatedBy: String = "defaultteam",
        sikkerhetsnivå: Int = 4
    ) =
        JsonMessage.newMessage(
            v1Map(ident,microfrontendId,JsonVersions.EnableMessage) +
            mapOf(
                "initiated_by" to initiatedBy,
                "sikkerhetsnivå" to sikkerhetsnivå
            )
        ).apply { JsonVersions.EnableMessage.addRequiredAndInterestedIn(this) }

    fun disableV2Message(ident: String, microfrontendId: String, initiatedBy: String) =
        JsonMessage.newMessage(
            v1Map(ident,microfrontendId,JsonVersions.DisableMessage) +
            mapOf("initiated_by" to initiatedBy)
        ).apply { JsonVersions.DisableMessage.addRequiredAndInterestedIn(this) }
}


fun legacyMessage(
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
      ${initiatedBy?.let { """ ,"@initiated_by": "$initiatedBy" """ } ?: ""}
    }
    """.trimIndent()


fun currentVersionMessage(
    action: String = "enabled",
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initiatedBy: String = "default-team"
) = JsonMessage.newMessage(
    currentVersionMap(
        action = action,
        microfrontendId = microfrontendId,
        ident = ident,
        sensitivitet = sensitivitet,
        initiatedBy = initiatedBy
    )
).toJson()

fun currentVersionPacket(
    messageRequirements: MessageRequirements = JsonVersions.EnableMessage,
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initatedBy: String = "default-team"
) =
    JsonMessage.newMessage(
        currentVersionMap(messageRequirements.action, microfrontendId, ident, sensitivitet, initatedBy)
    ).apply { messageRequirements.addRequiredAndInterestedIn(this) }

fun currentVersionMap(
    action: String = "enable",
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initiatedBy: String = "default-team"
) = mutableMapOf(
    "@action" to action,
    "ident" to ident,
    "microfrontend_id" to microfrontendId,
    "@initiated_by" to initiatedBy
).apply { if (action == "enable") this["sensitivitet"] to sensitivitet }


private fun MessageRequirements.addRequiredAndInterestedIn(jsonMessage: JsonMessage) {
    requireCommonKeys(jsonMessage)
    interestedInLegacyKeys(jsonMessage)
    interestedInCurrentVersionKeys(jsonMessage)
}