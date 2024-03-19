package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.statement.*

abstract class ResponseWithErrors private constructor() {
    private var errors: String? = null
    abstract val source: String

    constructor(error: String? = null, response: HttpResponse? = null, bodyAsText: String?= null) : this() {
        errors = (error ?: "") + (response?.let { "; Status fra ${it.request.url} er ${it.status} $bodyAsText" } ?: "")
    }

    fun errorMessage() = if (!errors.isNullOrEmpty()) errors.let { "Kall til $source feiler: $errors" } else null
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
    meldekortApiResponse: NullOrJsonNode? = null,
    errors: String? = null,
    response: HttpResponse? = null
) : ResponseWithErrors(errors, response) {
    //TODO
    override val source = "meldekort"
    val harMeldekort: Boolean =
        when {
            meldekortApiResponse == null -> false
            !meldekortApiResponse.hasContent() -> false
            else -> {
                meldekortApiResponse.int("etterregistrerteMeldekort") > 0
                        || meldekortApiResponse.intOrNull("meldekort")?.let { it > 0 } ?: false
                        || meldekortApiResponse.intOrNull("antallGjenstaaendeFeriedager")?.let { it > 0 } ?: false
                        || meldekortApiResponse.isNotNull("nesteMeldekort")
                        || meldekortApiResponse.isNotNull("nesteInnsendingAvMeldekort")
            }
        }
}

class ArbeidsøkerResponse(
    val erArbeidssoker: Boolean = false,
    val erStandard: Boolean = false,
    errors: String? = null,
    response: HttpResponse? = null

) : ResponseWithErrors(errors, response) {
    override val source = "aia-backend"
}

fun errorDetails(exception: Exception) {
    exception.stackTrace.firstOrNull()?.let { stacktraceElement ->
        """
                   Origin: ${stacktraceElement.fileName ?: "---"} ${stacktraceElement.methodName ?: "----"} linenumber:${stacktraceElement.lineNumber}
                   melding: "${exception::class.simpleName} ${exception.message?.let { ":$it" }}"
                """.trimIndent()
    } ?: "${exception::class.simpleName} ${exception.message?.let { ":$it" }}"
}