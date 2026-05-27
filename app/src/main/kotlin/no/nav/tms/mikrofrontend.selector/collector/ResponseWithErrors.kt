package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.*
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
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

abstract class ResponseWithErrors(private val errors: String?) {

    abstract val source: String

    fun errorMessage() = if (!errors.isNullOrEmpty()) errors.let { "Kall til $source feiler: $errors" } else null

    companion object {
        suspend inline fun <reified T : ResponseWithErrors> createFromHttpError(httpResponse: HttpResponse): T =
            createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "Status fra ${httpResponse.request.url} er ${httpResponse.status}",
                className = T::class.simpleName ?: "unknown"
            )

        inline fun <reified T : ResponseWithErrors> errorInJsonResponse(): T =
            createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "responsbody inneholdt ikke json",
                className = T::class.simpleName ?: "unknown"
            )


        fun <T : ResponseWithErrors> createWithError(
            constructor: KFunction<T>?,
            errorMessage: String,
            className: String
        ): T =
            constructor?.let {
                val params = constructor.parameters
                require(params.any { it.name == "errors" })

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
                    emptyList<Unit>()
                }
            }

            else -> throw IllegalArgumentException("No default value defined for parameter of type $this")
        }

    }
}


class MeldekortResponse(
    meldekortResponse: JsonPathInterpreter? = null,
    errors: String? = null,
) : ResponseWithErrors(errors) {
    override val source = "meldekort"
    val harMeldekort: Boolean =
        when {
            meldekortResponse == null -> false
            !meldekortResponse.hasContent() -> false
            else -> meldekortResponse.getOrNull<List<Any>>("meldekortTilUtfylling")?.isNotEmpty() ?: false
        }
}

class DigisosResponse(
    dokumenter: List<Tema>? = null,
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
