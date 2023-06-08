package no.nav.tms.microfrontend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.IllegalArgumentException


//Publish test
abstract class Action(protected val objectMapper: ObjectMapper) {

    var ident: String? = null
    var microfrontendId: String? = null
    var initiatedBy: String? = null
    abstract val action: String

    open fun map() =
        when {
            ident == null -> throw IllegalArgumentException("ident kan ikke være null i $action meldinger")
            ident?.any { !it.isDigit() } ?: false -> throw IllegalArgumentException("ident kan kun inneholde siffer")
            ident?.length != 11 -> throw IllegalArgumentException("ident må inneholde 11 siffer")
            microfrontendId == null -> throw IllegalArgumentException("microfrontend_id kan ikke være null i $action meldinger")
            initiatedBy == null -> throw IllegalArgumentException("initiated_by kan ikke være null i $action meldinger, bruk navnet til ditt team i gcp")
            else -> mutableMapOf(
                "@action" to action,
                "ident" to ident,
                "microfrontend_id" to microfrontendId,
                "initiated_by" to initiatedBy
            )
        }


    fun text(): String = objectMapper.writeValueAsString(map())
    fun jsonNode(): JsonNode = objectMapper.valueToTree(map())
}

class Enable(objectMapper: ObjectMapper) : Action(objectMapper) {
    override val action = "enable"
    var sikkerhetsnivå: Sikkerhetsnivå = Sikkerhetsnivå.NIVÅ_4

    override fun map() = super.map().apply {
        this["sikkerhetsnivå"] = sikkerhetsnivå.tallverdi
    }
}

class Disable(objectMapper: ObjectMapper) : Action(objectMapper) {
    override val action = "disable"
}


enum class Sikkerhetsnivå(val tallverdi: String) {
    NIVÅ_4("4"), NIVÅ_3("3")
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
        sikkerhetsnivå: Sikkerhetsnivå = Sikkerhetsnivå.NIVÅ_4
    ) =
        Enable(objectMapper).also { message ->
            message.ident = ident
            message.microfrontendId = microfrontendId
            message.initiatedBy = initiatedBy
            message.sikkerhetsnivå = sikkerhetsnivå
        }
}


