package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.MessageRequirements
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet.HIGH
import java.time.LocalDateTime

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}


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
    initiatedBy: String = "default-team"
) =
    JsonMessage.newMessage(
        currentVersionMap(messageRequirements, microfrontendId, ident, sensitivitet, initiatedBy)
    ).apply {
        messageRequirements.requireCommonKeys(this)
        messageRequirements.interestedInCurrentVersionKeys(this)
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

val testproduktkortCounter = ProduktkortCounter()
fun String.safTestDokument(sistEndret: LocalDateTime = LocalDateTime.now()) = Dokument(this, sistEndret)
