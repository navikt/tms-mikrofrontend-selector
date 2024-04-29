package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.MessageRequirements
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.HIGH
import java.time.LocalDateTime

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

fun testJsonString(
    messageRequirements: MessageRequirements = EnableMessage,
    microfrontendId: String,
    ident: String,
    levelOfAssurance: LevelOfAssurance = HIGH,
    initiatedBy: String = "default-team"
) = objectMapper.writeValueAsString(
    jsonTestMap(
        messageRequirements = messageRequirements,
        microfrontendId = microfrontendId,
        ident = ident,
        levelOfAssurance = levelOfAssurance,
        initiatedBy = initiatedBy
    )
)

fun testJsonMessage(
    messageRequirements: MessageRequirements = EnableMessage,
    microfrontendId: String,
    ident: String,
    levelOfAssurance: LevelOfAssurance = HIGH,
    initiatedBy: String = "default-team"
): JsonMessage =
    jsonTestMap(
        messageRequirements,
        microfrontendId,
        ident,
        levelOfAssurance,
        initiatedBy
    ).toJsonMessage()


fun jsonTestMap(
    messageRequirements: MessageRequirements,
    microfrontendId: String,
    ident: String,
    levelOfAssurance: LevelOfAssurance = HIGH,
    initiatedBy: String = "default-team"
) = mutableMapOf(
    //TODO: fiks i kafkalib
    "@event_name" to messageRequirements.action,
    "@action" to messageRequirements.action,
    "ident" to ident,
    "microfrontend_id" to microfrontendId,
    "@initiated_by" to initiatedBy
).apply {
    if (messageRequirements == EnableMessage)
        this["sensitivitet"] = levelOfAssurance.name.lowercase()
}

val testproduktkortCounter = ProduktkortCounter()
fun String.safTestDokument(sistEndret: LocalDateTime = LocalDateTime.now()) = Dokument(this, sistEndret)

fun setupBroadcaster(personRepository: PersonRepository) = MessageBroadcaster(
    listOf(
        EnableSubscriber(personRepository),
        DisableSubscriber(personRepository)
    )
)

fun <K, V> Map<out K, V>.toJsonMessage() = JsonMessage.fromJson(objectMapper.writeValueAsString(this), null)

