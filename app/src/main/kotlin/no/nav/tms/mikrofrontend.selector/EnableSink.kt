package no.nav.tms.mikrofrontend.selector

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

class EnableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@action", EnableMessage.action) }
            validate { message ->  EnableMessage.requireCommonKeys(message) }
            validate { message -> EnableMessage.interestedInCurrentVersionKeys(message)}
            validate { message -> EnableMessage.interestedInLegacyKeys(message)}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info { "mottok enablemelding for ${packet.microfrontendId}" }
        EnableMessage.countVersion(packet)
        personRepository.enableMicrofrontend(packet)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info(problems.toString())
    }
}

