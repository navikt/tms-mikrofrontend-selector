package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.tms.common.metrics.installTmsApiMetrics
import no.nav.tms.common.observability.ApiMdc
import no.nav.tms.mikrofrontend.selector.collector.ApiException
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher.TokenFetcherException
import no.nav.tms.mikrofrontend.selector.database.DatabaseException
import no.nav.tms.token.support.tokenx.validation.tokenX
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.text.DateFormat

internal fun Application.selectorApi(
    personalContentCollector: PersonalContentCollector,
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
            when (cause) {
                is DatabaseException -> {
                    log.error { "Feil i henting av microfrontends" }
                    secureLog.warn(cause.originalException) { """Feil i henting av microfrontends for ${cause.ident}}""".trimMargin() }
                    call.respond(HttpStatusCode.InternalServerError)

                }

                is ApiException -> {
                    log.warn {
                        "${cause::class.simpleName?:"ApiException"}: ${cause.message}" }
                    call.respond(HttpStatusCode.ServiceUnavailable)

                }

                is TokenFetcherException -> {
                    log.warn { "TokenFetcherException: ${cause.message}" }
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }

                else -> {
                    log.error { "Ukjent feil ved henting av microfrontends: ${cause.message} ${cause.javaClass.name}" }
                    secureLog.error(cause) { "Ukjent feil ved henting av microfrontends" }
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

        }
    }

    installTmsApiMetrics {
        setupMetricsRoute = false
    }
    install(ApiMdc)

    routing {
        authenticate {
            route("din-oversikt") {
                get {
                    val user = TokenXUserFactory.createTokenXUser(call)
                    val content = personalContentCollector.getContent(user, user.levelOfAssurance)
                    content.errors?.takeIf { it.isNotEmpty() }?.let {
                        log.warn { it }
                    }
                    call.respond(
                        status = content.resolveStatus(),
                        content
                    )
                }
            }
        }
    }
}

private fun installAuth(): Application.() -> Unit = {
    authentication {
        tokenX {
            setAsDefault = true
        }
    }
}
