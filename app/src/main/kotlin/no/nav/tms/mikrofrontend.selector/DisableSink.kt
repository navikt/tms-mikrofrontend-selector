package no.nav.tms.mikrofrontend.selector

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.*
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.initiatedBy
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.traceInfo
import observability.Contenttype
import observability.traceMicrofrontend

class DisableSink(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
) :
    River.PacketListener {

    private val log = KotlinLogging.logger {}
    private val secureLog = KotlinLogging.logger("secureLog")

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@action", DisableMessage.action) }
            validate { DisableMessage.requireCommonKeys(it) }
            validate { DisableMessage.interestedInCurrentVersionKeys(it) }
            validate { DisableMessage.interestedInLegacyKeys(it) }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        traceMicrofrontend(id = packet.microfrontendId, extra = packet.traceInfo("disable")) {
            try {
                log.info { "disablemelding mottat fra ${packet.initiatedBy}" }
                personRepository.disableMicrofrontend(packet)
                DisableMessage.countVersion(packet)
            } catch (e: Exception) {
                log.error { "Feil i behandling av enablemelding" }
                secureLog.error(e) { """
                    Feil i behandling av enablemelding for person med ident ${packet.ident}
                    """.trimIndent() }
                log.error { e.message }
            }

        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}

