package no.nav.tms.mikrofrontend.selector

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.mikrofrontend.selector.database.JsonVersions.DisableKeys
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

class DisableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@action", DisableKeys.action) }
            validate { DisableKeys.requireCommonKeys(it) }
            validate { DisableKeys.interestedInCurrentVersionKeys(it) }
            validate { DisableKeys.interestedInLegacyKeys(it) }

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

