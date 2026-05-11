package no.nav.tms.mikrofrontend.selector

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.common.logging.TeamLogs
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.ident
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage

class DisableSubscriber(
    private val personRepository: PersonRepository
) : Subscriber() {

    private val log = KotlinLogging.logger {}
    private val teamLog = TeamLogs.logger { }
    override fun subscribe(): Subscription = Subscription
        .forEvent(DisableMessage.action)
        .withFields(*DisableMessage.requiredFields)

    override suspend fun receive(jsonMessage: JsonMessage) {
        try {
            log.info { "disable-melding mottat" }
            personRepository.disableMicrofrontend(jsonMessage)
        } catch (e: Exception) {
            log.error { "Feil i behandling av disable-melding" }
            teamLog.error(e) { "Feil i behandling av disable-melding for person [${jsonMessage.ident}]" }
        }
    }
}

