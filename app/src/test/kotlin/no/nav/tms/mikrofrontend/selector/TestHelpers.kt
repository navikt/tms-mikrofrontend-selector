package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import no.nav.tms.kafka.application.EventMetadata
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.KafkaEvent
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.mikrofrontend.selector.collector.Tema
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.MessageRequirements
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.reflect.full.primaryConstructor

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

fun testJsonString(
    messageRequirements: MessageRequirements = EnableMessage,
    microfrontendId: String,
    ident: String,
    levelOfAssurance: LevelOfAssurance = LevelOfAssurance.High,
    initiatedBy: String = "default-team"
) = objectMapper.writeValueAsString(
    jsonTestMap(
        messageRequirements = messageRequirements,
        microfrontendId = microfrontendId,
        ident = ident,
        levelOfAssurance = levelOfAssurance,
        initiatedBy = initiatedBy
    )
)

fun testJsonMessage(
    messageRequirements: MessageRequirements = EnableMessage,
    microfrontendId: String,
    ident: String,
    levelOfAssurance: LevelOfAssurance = LevelOfAssurance.High,
    initiatedBy: String = "default-team"
): JsonMessage =
    jsonTestMap(
        messageRequirements,
        microfrontendId,
        ident,
        levelOfAssurance,
        initiatedBy
    ).toJsonMessage()


fun jsonTestMap(
    messageRequirements: MessageRequirements,
    microfrontendId: String,
    ident: String,
    levelOfAssurance: LevelOfAssurance = LevelOfAssurance.High,
    initiatedBy: String = "default-team"
) = mutableMapOf(
    //TODO: fiks i kafkalib
//    "@event_name" to messageRequirements.action,
    "@action" to messageRequirements.action,
    "ident" to ident,
    "microfrontend_id" to microfrontendId,
    "@initiated_by" to initiatedBy
).apply {
    if (messageRequirements == EnableMessage)
        this["sensitivitet"] = levelOfAssurance.name.lowercase()
}

fun String.safTestDokument(sistEndret: LocalDateTime = LocalDateTime.now()) = Tema(
    this,
    url = if (this == "DAG") "https://www.intern.dev.nav.no/dokumentarkiv/tema" else "https://www.nav.no",
    navn = "Dagpenger",
    sistEndret = sistEndret
)

fun setupBroadcaster(personRepository: PersonRepository) = MessageBroadcaster(
    listOf(
        EnableSubscriber(personRepository),
        DisableSubscriber(personRepository)
    ),
    listOf("@action")
)

fun <K, V> Map<out K, V>.toJsonMessage(): JsonMessage {
    val jsonNode: JsonNode = this.let { objectMapper.valueToTree(this) }
    val constructor = JsonMessage::class.primaryConstructor?:throw IllegalArgumentException("class has no primaryconstructor")
    val args = constructor.parameters.map { parameter ->
        when {
            parameter.name == "eventName" -> "@action"
            parameter.name == "json" -> jsonNode
            parameter.name == "metadata" -> EventMetadata(
                topic = "Tevita",
                kafkaEvent = KafkaEvent(key = "Cassaundra", value = "Sheina"),
                createdAt = null,
                readAt = ZonedDateTime.now()
            )
            else -> throw IllegalArgumentException("unexpected Ktype for parameter errors: ${parameter.type}")
        }
    }.toTypedArray()

    return constructor.call(*args)
}

suspend fun HttpResponse.bodyAsJson() = objectMapper.readTree(bodyAsText())
inline fun <reified T> JsonNode.parseOrNull(name: String): T? {
    val node = this[name]

    return if (node.isNull || node.isEmpty) {
        null
    } else {
        jacksonObjectMapper().treeToValue(this, T::class.java)
    }
}
