package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.*
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.redactedMessage
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

val log = KotlinLogging.logger{}

abstract class ResponseWithErrors(private val errors: String?) {

    abstract val source: String

    fun errorMessage() = if (!errors.isNullOrEmpty()) errors.let { "Kall til $source feiler: $errors" } else null

    companion object {
        suspend inline fun <reified T : ResponseWithErrors> createFromHttpError(httpResponse: HttpResponse): T =
            createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = " Status fra ${httpResponse.request.url} er ${httpResponse.status} ${httpResponse.bodyAsText()}",
                className = T::class.simpleName ?: "unknown"
            )

        inline fun <reified T : ResponseWithErrors> errorInJsonResponse(textInBody: String): T =
            createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "responsbody inneholder ikke json: ${textInBody.redactedMessage()}",
                className = T::class.simpleName ?: "unknown"
            )

        fun <T> createWithError(constructor: KFunction<T>?, errorMessage: String, className: String): T =
            constructor?.let {
                val params = constructor.parameters
                val args = params.map { parameter ->
                    when {
                        parameter.name != "errors" -> parameter.type.default()
                        parameter.name == "errors" && parameter.type.isOfType(String::class.starProjectedType) -> errorMessage
                        parameter.name == "errors" && parameter.type.isListType(String::class.starProjectedType) -> listOf(
                            errorMessage
                        )

                        else -> throw IllegalArgumentException("unexpected Ktype for parameter errors: ${parameter.type}")
                    }
                }.toTypedArray()
                constructor.call(*args)
            } ?: throw IllegalArgumentException("$className does not have a primary constructor")

        private fun KType.isListType(starProjectedType: KType): Boolean {
            if (!this.isSubtypeOf(List::class.starProjectedType)
                && !this.isSubtypeOf(
                    List::class.starProjectedType.withNullability(true)
                )
            )
                return false
            val elementType =
                this.arguments.first().type ?: throw IllegalArgumentException("List type argument not found")
            return elementType == starProjectedType || elementType == starProjectedType.withNullability(true)
        }

        private fun KType.isOfType(starProjectedType: KType) =
            this == starProjectedType || this == starProjectedType.withNullability(true)


        private fun KType.default(): Any? = when {
            this.isMarkedNullable -> null
            this == String::class.starProjectedType -> ""
            this == Int::class.starProjectedType -> 0
            this.isSubtypeOf(List::class.starProjectedType) -> {
                val elementType =
                    this.arguments.first().type ?: throw IllegalArgumentException("List type argument not found")
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
    sakstemakoder: List<String>? = null,
    errors: List<String>? = null
) : ResponseWithErrors(errors?.joinToString(";")) {
    val sakstemakoder = sakstemakoder ?: emptyList()
    override val source: String = "SAF"
}

class OppfolgingResponse(
    underOppfolging: Boolean? = false,
    error: String? = null,
) : ResponseWithErrors(error) {
    val underOppfolging: Boolean = underOppfolging ?: false
    override val source = "Oppfølgingapi"
}

class MeldekortResponse(
    meldekortApiResponse: JsonPathInterpreter? = null,
    errors: String? = null,
) : ResponseWithErrors(errors) {
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
    val erArbeidssoker: Boolean? = null,
    val erStandard: Boolean? = null,
    errors: String? = null
) : ResponseWithErrors(errors) {
    fun isStandardInnsats(): Boolean = erArbeidssoker == true && erStandard == true

    override val source = "aia-backend"
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

fun errorDetails(exception: Exception) =
    exception.stackTrace.firstOrNull()?.let { stacktraceElement ->
        """
                   Origin: ${stacktraceElement.fileName ?: "---"} ${stacktraceElement.methodName ?: "----"} linenumber:${stacktraceElement.lineNumber}
                   melding: "${exception::class.simpleName} ${exception.message?.let { ":$it" }}"
                """.trimIndent()
    } ?: "${exception::class.simpleName} ${exception.message?.let { ":$it" }}"


