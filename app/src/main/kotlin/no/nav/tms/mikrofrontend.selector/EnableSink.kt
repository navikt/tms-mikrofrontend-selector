package no.nav.tms.mikrofrontend.selector

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.*
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.initiatedBy
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.traceInfo
import observability.Contenttype
import observability.traceMicrofrontend

class EnableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}
    private val secureLog = KotlinLogging.logger("secureLog")

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@action", EnableMessage.action) }
            validate { message -> EnableMessage.requireCommonKeys(message) }
            validate { message -> EnableMessage.interestedInCurrentVersionKeys(message) }
            validate { message -> EnableMessage.interestedInLegacyKeys(message) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        traceMicrofrontend(id = packet.microfrontendId, extra = packet.traceInfo("enable")) {
            try {
                log.info { "Enablemelding motatt" }
                //TODO
                secureLog.info { "Enablet for person med ident ${packet.ident}" }
                EnableMessage.countVersion(packet)
                personRepository.enableMicrofrontend(packet)
            } catch (e: Exception) {
                log.error { "Feil i behandling av enablemelding" }
                secureLog.error { "Feil i behandling av enablemelding for person med ident ${packet.ident}" }
                log.error { e.message }
            }
        }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        withMDC(mapOf("contenttype" to Contenttype.microfrontend.name, "sink" to "enable")) {
            log.info { error.problems }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}