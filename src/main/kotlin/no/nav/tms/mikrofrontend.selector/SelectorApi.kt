package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import no.nav.tms.mikrofrontend.selector.database.DatabaseException
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.metrics
import no.nav.tms.token.support.authentication.installer.installAuthenticators
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.text.DateFormat

internal fun Application.selectorApi(
    personRepository: PersonRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    val secureLog = KotlinLogging.logger("secureLog")

    installAuthenticatorsFunction()

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            dateFormat = DateFormat.getDateTimeInstance()
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is DatabaseException) {
                secureLog.warn { "Feil i henting av microfrontends for ${cause.ident}\n ${cause.originalException.stackTrace}" }
                call.respond(HttpStatusCode.InternalServerError)

            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }

        }
    }
    routing {
        metrics(prometheusMeterRegistry)
        authenticate {
            route("mikrofrontends") {
                get() {
                    val user = TokenXUserFactory.createTokenXUser(call)
                    call.respond(personRepository.getEnabledMicrofrontends(user.ident,user.loginLevel))
                }
            }
        }
    }
}

private fun installAuth(): Application.() -> Unit = {
    installAuthenticators {
        installTokenXAuth {
            setAsDefault = true
        }
    }
}
