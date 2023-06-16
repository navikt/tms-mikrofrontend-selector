package no.nav.tms.mikrofrontend.selector

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

class DisableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@action", DisableMessage.action) }
            validate { DisableMessage.requireCommonKeys(it) }
            validate { DisableMessage.interestedInCurrentVersionKeys(it) }
            validate { DisableMessage.interestedInLegacyKeys(it) }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info { "mottok disablemelding for ${packet.microfrontendId}" }
        personRepository.disableMicrofrontend(packet)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info(problems.toString())
    }
}

