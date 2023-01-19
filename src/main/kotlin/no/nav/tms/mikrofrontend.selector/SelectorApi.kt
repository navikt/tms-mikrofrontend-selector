package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.tms.token.support.authentication.installer.installAuthenticators
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.metrics
import java.text.DateFormat
import java.util.*

internal fun Application.selectorApi(
    personRepository: PersonRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    installAuthenticatorsFunction()

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            dateFormat = DateFormat.getDateTimeInstance()
        }
    }

    routing {
        metrics(prometheusMeterRegistry)
        authenticate {
            route("mikrofrontends") {
                get(){
                    call.respond(personRepository.getEnabledMicrofrontends(userIdent))
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


private val PipelineContext<Unit, ApplicationCall>.userIdent get() = TokenXUserFactory.createTokenXUser(call).ident

