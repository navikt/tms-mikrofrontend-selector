import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.database.JsonVersions
import no.nav.tms.mikrofrontend.selector.database.KeyRequirements
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.HIGH

object LegacyJsonMessages {
    fun v1Message(ident: String, microfrontendId: String, keyRequirements: KeyRequirements) =
        JsonMessage.newMessage(
            mapOf(
                "@action" to keyRequirements.action,
                "ident" to ident,
                "microfrontend_id" to microfrontendId
            )
        ).apply {
            keyRequirements.requireCommonKeys(this)
            keyRequirements.interestedInLegacyKeys(this)
            keyRequirements.interestedInCurrentVersionKeys(this)
        }

    fun enableV2Message(ident: String, microfrontendId: String, initiatedBy: String = "defaultteam", sikkerhetsnivå: Int = 4) =
        JsonMessage.newMessage(
            mapOf(
                "ident" to ident,
                "microfrontend_id" to microfrontendId,
                "initiated_by" to initiatedBy,
                "sikkerhetsnivå" to sikkerhetsnivå
            )
        ).apply {
            JsonVersions.EnableKeys.requireCommonKeys(this)
            JsonVersions.EnableKeys.interestedInLegacyKeys(this)
            JsonVersions.EnableKeys.interestedInCurrentVersionKeys(this)
        }

    fun disableV2Message(ident: String, microfrontendId: String, initiatedBy: String) =
        JsonMessage.newMessage(
            mapOf(
                "ident" to ident,
                "microfrontend_id" to microfrontendId,
                "initiated_by" to initiatedBy
            )
        ).apply {
            JsonVersions.EnableKeys.requireCommonKeys(this)
            JsonVersions.EnableKeys.interestedInCurrentVersionKeys(this)
            JsonVersions.EnableKeys.interestedInLegacyKeys(this)
        }
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
    keyRequirements: KeyRequirements = JsonVersions.EnableKeys,
    microfrontendId: String,
    ident: String,
    sensitivitet: Sensitivitet = HIGH,
    initatedBy: String = "default-team"
) =
    JsonMessage.newMessage(
        currentVersionMap(keyRequirements.action, microfrontendId, ident, sensitivitet, initatedBy)
    ).also { keyRequirements.addRequiredAndInterestedIn(it) }

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
    "@initiated_by" to initiatedBy).apply { if (action == "enable") this["sensitivitet"] to sensitivitet }

fun disableMessage(microfrontendId: String, fnr: String, initiatedBy: String = "default-team") = """
    {
      "@action": "disable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId",
      "@initiated_by": "$initiatedBy"
}
    """.trimIndent()

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

fun KeyRequirements.addRequiredAndInterestedIn(jsonMessage: JsonMessage) {
    requireCommonKeys(jsonMessage)
    interestedInLegacyKeys(jsonMessage)
    interestedInCurrentVersionKeys(jsonMessage)
}