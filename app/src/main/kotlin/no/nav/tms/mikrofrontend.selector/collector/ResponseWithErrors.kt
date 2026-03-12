package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.*
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.redactedMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

val log = KotlinLogging.logger {}

/**
 * Base class for responses that may contain errors from external services.
 * Subclasses must have a primary constructor with an "errors" parameter.
 */
abstract class ResponseWithErrors(private val errors: String?) {

    abstract val source: String

    fun errorMessage(): String? = 
        if (!errors.isNullOrEmpty()) "Kall til $source feiler: $errors" else null

    companion object {
        /**
         * Creates an error response from an HTTP error status.
         */
        suspend inline fun <reified T : ResponseWithErrors> createFromHttpError(httpResponse: HttpResponse): T =
            createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = " Status fra ${httpResponse.request.url} er ${httpResponse.status} ${httpResponse.bodyAsText()}",
                className = T::class.simpleName ?: "unknown"
            )

        /**
         * Creates an error response when the response body is not valid JSON.
         */
        inline fun <reified T : ResponseWithErrors> errorInJsonResponse(textInBody: String): T =
            createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "responsbody inneholder ikke json: ${textInBody.redactedMessage()}",
                className = T::class.simpleName ?: "unknown"
            )

        /**
         * Creates an error response using reflection.
         * The target class must have a primary constructor with an "errors" parameter.
         */
        fun <T : ResponseWithErrors> createWithError(
            constructor: KFunction<T>?,
            errorMessage: String,
            className: String
        ): T {
            requireNotNull(constructor) { "$className does not have a primary constructor" }
            
            val params = constructor.parameters
            require(params.any { it.name == "errors" }) { "$className must have an 'errors' parameter" }

            val args = params.map { parameter ->
                when {
                    parameter.name != "errors" -> parameter.type.defaultValue()
                    parameter.type.isStringType() -> errorMessage
                    parameter.type.isStringListType() -> listOf(errorMessage)
                    else -> throw IllegalArgumentException("Unexpected type for 'errors' parameter: ${parameter.type}")
                }
            }.toTypedArray()
            
            return constructor.call(*args)
        }

        private fun KType.isStringType(): Boolean =
            this == String::class.starProjectedType || 
            this == String::class.starProjectedType.withNullability(true)

        private fun KType.isStringListType(): Boolean {
            if (!this.isSubtypeOf(List::class.starProjectedType) &&
                !this.isSubtypeOf(List::class.starProjectedType.withNullability(true))
            ) return false
            
            val elementType = this.arguments.firstOrNull()?.type 
                ?: throw IllegalArgumentException("List type argument not found")
            return elementType == String::class.starProjectedType || 
                   elementType == String::class.starProjectedType.withNullability(true)
        }

        private fun KType.defaultValue(): Any? = when {
            this.isMarkedNullable -> null
            this == String::class.starProjectedType -> ""
            this == Int::class.starProjectedType -> 0
            this.isSubtypeOf(List::class.starProjectedType) -> {
                val elementType = this.arguments.firstOrNull()?.type
                    ?: throw IllegalArgumentException("List type argument not found")
                if (elementType == String::class.starProjectedType) {
                    emptyList<String>()
                } else {
                    throw IllegalArgumentException("No default value defined for list of type $elementType")
                }
            }
            else -> throw IllegalArgumentException("No default value defined for parameter of type $this")
        }

    }
}


class SafResponse(
    dokumenter: List<Dokument>? = null,
    errors: List<String>? = null
) : ResponseWithErrors(errors?.joinToString(";")) {
    val dokumenter = dokumenter ?: emptyList()
    override val source: String = "SAF"
}

class Dokument(
    val kode: String,
    dokumentarkivUrlResolver: DokumentarkivUrlResolver,
    val sistEndret: LocalDateTime,
    val navn: String,
) {
    val url = dokumentarkivUrlResolver.urlFor(kode)
    
    companion object {
        /** Maximum number of documents to return from getLatest() */
        private const val MAX_LATEST_DOCUMENTS = 2
        
        fun List<Dokument>.getLatest(): List<Dokument> =
            sortedByDescending { it.sistEndret }.take(MAX_LATEST_DOCUMENTS)
    }
}

class MeldekortResponse(
    meldekortResponse: JsonPathInterpreter? = null,
    errors: String? = null,
) : ResponseWithErrors(errors) {
    override val source = "meldekort"
    
    val harMeldekort: Boolean = meldekortResponse?.let { response ->
        response.hasContent() && (
            response.intOrNull("etterregistrerteMeldekort")?.let { it > 0 } ?: false
            || response.intOrNull("meldekort")?.let { it > 0 } ?: false
            || response.intOrNull("antallGjenstaaendeFeriedager")?.let { it > 0 } ?: false
            || response.isNotNull("nesteMeldekort")
            || response.isNotNull("nesteInnsendingAvMeldekort")
        )
    } ?: false
}

class PdlResponse(
    val fødselsdato: LocalDate? = null,
    val fødselsår: Int?,
    errors: List<String>? = null,
) : ResponseWithErrors(errors?.joinToString(";")) {
    override val source = "pdl"
    fun calculateAge() = when {
        fødselsdato != null -> ChronoUnit.YEARS.between(fødselsdato, LocalDate.now()).toInt()
        fødselsår != null -> LocalDate.now().year - fødselsår
        else -> 0
    }
}

class DigisosResponse(
    dokumenter: List<Dokument>? = null,
    errors: List<String>? = null,
) : ResponseWithErrors(errors?.joinToString(";")) {
    override val source = "digisos"
    val dokumenter = dokumenter ?: emptyList()

}


fun errorDetails(exception: Exception) =
    exception.stackTrace.firstOrNull()?.let { stacktraceElement ->
        """
                   Origin: ${stacktraceElement.fileName ?: "---"} ${stacktraceElement.methodName ?: "----"} linenumber:${stacktraceElement.lineNumber}
                   melding: "${exception::class.simpleName} ${exception.message?.let { ":$it" }}"
                """.trimIndent()
    } ?: "${exception::class.simpleName} ${exception.message?.let { ":$it" }}"
