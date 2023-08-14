package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import nav.no.tms.common.metrics.installTmsApiMetrics
import no.nav.tms.mikrofrontend.selector.database.DatabaseException
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.getManifestBucketContent
import no.nav.tms.token.support.authentication.installer.installAuthenticators
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.text.DateFormat

internal fun Application.selectorApi(
    personRepository: PersonRepository,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    val secureLog = KotlinLogging.logger("secureLog")
    val log = KotlinLogging.logger {}

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
                log.error { "Ukjent feil ved henting av microfrontends" }
                call.respond(HttpStatusCode.InternalServerError)
            }

        }
    }

    installTmsApiMetrics {
        setupMetricsRoute = false
    }

    routing {
        authenticate {
            route("mikrofrontends") {
                get() {
                    val user = TokenXUserFactory.createTokenXUser(call)
                    call.respond(
                        personRepository.getEnabledMicrofrontends(user.ident)?.apiResponseV1(user.loginLevel)
                            ?: Microfrontends.emptyApiResponse()
                    )
                }
            }

            route("microfrontends") {
                get() {
                    val user = TokenXUserFactory.createTokenXUser(call)
                    val manifests = getManifestBucketContent()
                    call.respond(
                        personRepository.getEnabledMicrofrontends(user.ident)?.apiResponseV2(user.loginLevel, manifests)
                            ?: Microfrontends.emptyApiResponse()
                    )
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
