package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.statement.*

abstract class ResponseWithErrors private constructor() {
    private var errors: String? = null
    abstract val source: String

    constructor(error: String? = null, response: HttpResponse? = null) : this() {
        errors = (error ?: "") + (response?.let { "; Status fra ${it.request.url} er ${it.status}" } ?: "")
    }

    fun errorMessage() = if(!errors.isNullOrEmpty())errors.let { "Kall til $source feiler: $errors" } else null
}

class SafResponse(
    sakstemakoder: List<String>? = null,
    errors: List<String>? = null,
    response: HttpResponse? = null
) : ResponseWithErrors(errors?.joinToString(";"), response) {
    val sakstemakoder = sakstemakoder ?: emptyList()
    override val source: String = "SAF"
}

class OppfolgingResponse(
    underOppfolging: Boolean? = false,
    error: String? = null,
    response: HttpResponse? = null
) :
    ResponseWithErrors(error, response) {
    val underOppfolging: Boolean = underOppfolging ?: false
    override val source = "Oppfølgingapi"
}

class MeldekortResponse(
    val todo: Boolean = false,
    errors: String? = null,
    response: HttpResponse? = null
) : ResponseWithErrors(errors, response) {
    //TODO
    override val source = "meldekort"
    fun harMeldekort(): Boolean = false
}

class ArbeidsøkerResponse(
    val erArbeidssoker: Boolean = false,
    val erStandard: Boolean = false,
    errors: String? = null,
    response: HttpResponse? = null

) : ResponseWithErrors(errors, response) {
    override val source = "aia-backend"
}