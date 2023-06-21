package no.nav.tms.microfrontend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.IllegalArgumentException


private const val currentVersion = "3"
abstract class Action(protected val objectMapper: ObjectMapper) {

    var ident: String? = null
    var microfrontendId: String? = null
    var initiatedBy: String = System.getenv("NAIS_NAMESPACE")?:""
    abstract val action: String

    open fun map() =
        when {
            ident == null -> throw IllegalArgumentException("ident kan ikke være null i $action meldinger")
            ident?.any { !it.isDigit() } ?: false -> throw IllegalArgumentException("ident kan kun inneholde siffer")
            ident?.length != 11 -> throw IllegalArgumentException("ident må inneholde 11 siffer")
            microfrontendId == null -> throw IllegalArgumentException("microfrontend_id kan ikke være null i $action meldinger")
            initiatedBy == "" -> throw IllegalArgumentException("initiated_by kan ikke være null i $action meldinger, bruk navnet til ditt team i gcp")
            else -> mutableMapOf(
                "@version" to currentVersion,
                "@action" to action,
                "ident" to ident,
                "microfrontend_id" to microfrontendId,
                "@initiated_by" to initiatedBy
            )
        }


    fun text(): String = objectMapper.writeValueAsString(map())
    fun jsonNode(): JsonNode = objectMapper.valueToTree(map())
}

class Enable(objectMapper: ObjectMapper) : Action(objectMapper) {
    override val action = "enable"
    var sensitivitet: Sensitivitet = Sensitivitet.HIGH

    override fun map() = super.map().apply {
        this["sensitivitet"] = sensitivitet.name.lowercase()
    }
}

class Disable(objectMapper: ObjectMapper) : Action(objectMapper) {
    override val action = "disable"
}

enum class Sensitivitet{
    HIGH, SUBSTANTIAL;
    val stringValue = name.lowercase()

}

object MessageBuilder {
    private val objectMapper = ObjectMapper()

    fun disable(properties: Action.() -> Unit): Disable =
        Disable(objectMapper).also(properties)

    fun disable(ident: String, microfrontenId: String, initiatedBy: String) =
        Disable(objectMapper).also { message ->
            message.ident = ident
            message.microfrontendId = microfrontenId
            message.initiatedBy = initiatedBy
        }

    fun enable(properties: Enable.() -> Unit): Enable =
        Enable(objectMapper).also(properties)

    fun enable(
        ident: String,
        microfrontendId: String,
        initiatedBy: String,
        sensitivitet: Sensitivitet = Sensitivitet.HIGH
    ) =
        Enable(objectMapper).also { message ->
            message.ident = ident
            message.microfrontendId = microfrontendId
            message.initiatedBy = initiatedBy
            message.sensitivitet = sensitivitet
        }
}


