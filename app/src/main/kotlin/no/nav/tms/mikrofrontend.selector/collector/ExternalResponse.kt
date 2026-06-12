package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging

val log = KotlinLogging.logger {}

enum class ExternalService {
    Digisos, Saf, Pdl, MeldekortApi, DpMeldekort
}

class ExternalResponse<out T> private constructor(
    val service: ExternalService,
    val value: T,
    val isError: Boolean,
    private val errorMessage: String?,
    private val cause: Exception?
) {
    fun getErrorMessage(): String {
        if (!isError) {
            throw IllegalStateException("Kan ikke hente feilmelding fra vellykket response")
        }

        return errorMessage!!
    }

    fun getCause(): Exception? {
        if (!isError) {
            throw IllegalStateException("Kan ikke hente feilkilde fra vellykket response")
        }

        return cause
    }

    companion object {
        fun <T> ok(service: ExternalService, value: T) = ExternalResponse(service, value, false, null, null)
        fun <T> error(default: T, service: ExternalService, message: String) =
            ExternalResponse(service, default, true , message, null)
        fun <T> error(default: T, service: ExternalService, message: String, cause: Exception?) =
            ExternalResponse(service, default, true, message, cause)
    }
}
