package no.nav.tms.mikrofrontend.selector

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.mikrofrontend.selector.database.JsonVersions
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

class EnableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@action", "enable") }
            validate { message ->  JsonVersions.Enabled.setRequiredKeys(message) }
            validate { message -> JsonVersions.Enabled.setInterestedInKeys(message)}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info { "mottok enablemelding for ${packet.microfrontendId}" }
        personRepository.enableMicrofrontend(packet)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info(problems.toString())
    }
}

fun JsonMessage.requireKeys(requiredKeys: List<String>) =
    requiredKeys.forEach { key -> requireKey(key) }

fun JsonMessage.interestedInKeys(interestedInKeys: List<String>) =
    interestedInKeys.forEach { key -> key }

