package no.nav.tms.mikrofrontend.selector

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.common.observability.traceMicrofrontend
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.traceInfo

class EnableSubscriber(
    private val personRepository: PersonRepository
) : Subscriber() {

    private val log = KotlinLogging.logger {}
    private val secureLog = KotlinLogging.logger("secureLog")

    override fun subscribe(): Subscription = Subscription
        .forEvent(EnableMessage.action)
        .withFields(*EnableMessage.requiredFields)


    override suspend fun receive(jsonMessage: JsonMessage) {
        traceMicrofrontend(id = jsonMessage.microfrontendId, extra = jsonMessage.traceInfo("enable")) {
            try {
                log.info { "Enablemelding motatt" }
                personRepository.enableMicrofrontend(jsonMessage)
            } catch (e: Exception) {
                log.error { "Feil i behandling av enablemelding" }
                secureLog.error(e) {
                    """
                    Feil i behandling av enablemelding for person med ident ${jsonMessage.ident}
                    """.trimIndent()
                }
                log.error { e.message }
            }
        }
    }
}