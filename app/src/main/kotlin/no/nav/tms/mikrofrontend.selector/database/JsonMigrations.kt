package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.tms.mikrofrontend.selector.database.Microfrontends.Companion.microfrontendMapper

private val log = KotlinLogging.logger { }

object JsonVersions {

    private fun latestVersion(id: String, sensitivitet: Sensitivitet) = microfrontendMapper.readTree(
        """
         {
            "microfrontend_id": "$id",
            "sensitivitet" : "${sensitivitet.name}"
        }
      """.trimMargin()
    )

    fun konverterTilObjekter(jsonNode: JsonNode): JsonNode = latestVersion(jsonNode.asText(),Sensitivitet.HIGH)
    fun migrerFraSikkerhetsnivåTilSensitivitet(jsonNode: JsonNode): JsonNode =
        latestVersion(jsonNode.mikrofrontendId, Sensitivitet.resolve(jsonNode.sikkerhetsnivå))

    fun JsonNode.erVersjonMedSikkerhetsnivå(): Boolean = this.get("sikkerhetsnivå") != null

    fun JsonNode.migrateToLatestVersion(): JsonNode = when {
        erVersjonMedSikkerhetsnivå() -> migrerFraSikkerhetsnivåTilSensitivitet(this)
        isValueNode -> konverterTilObjekter(this)
        else -> this
    }
}

fun test() {
    Sensitivitet.HIGH > Sensitivitet.SUBSTANTIAL
}

enum class Sensitivitet(val korresponderendeSikkerhetsnivå: Int) {
    HIGH(4), SUBSTANTIAL(3);

    fun innholdKanVises(other: Sensitivitet): Boolean =
        korresponderendeSikkerhetsnivå >= other.korresponderendeSikkerhetsnivå

    fun innholdKanVises(other: Int): Boolean =
        korresponderendeSikkerhetsnivå >= resolve(other).korresponderendeSikkerhetsnivå

    fun innholdKanVises(s: String) = korresponderendeSikkerhetsnivå >= valueOf(s).korresponderendeSikkerhetsnivå


    companion object {
        fun resolve(sikkerhetsnivå: Int?) = when (sikkerhetsnivå) {
            null -> HIGH
            4 -> HIGH
            3 -> SUBSTANTIAL
            else -> {
                log.error { "$sikkerhetsnivå har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
                HIGH
            }
        }
    }
}

private val JsonNode.mikrofrontendId: String
    get() = this["microfrontend_id"].asText()

private val JsonNode.sikkerhetsnivå: Int
    get() = this["sikkerhetsnivå"].asInt()

