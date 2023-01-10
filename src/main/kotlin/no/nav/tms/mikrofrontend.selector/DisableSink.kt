package no.nav.tms.mikrofrontend.selector

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

class DisableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "disable") }
            validate { it.requireKey("ident", "microfrontend_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        personRepository.disableMicrofrontend(packet.ident,packet.microfrontendId)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info(problems.toString())
    }
}

