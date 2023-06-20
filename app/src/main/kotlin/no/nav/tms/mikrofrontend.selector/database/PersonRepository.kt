package no.nav.tms.mikrofrontend.selector.database

import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.ident
import no.nav.tms.mikrofrontend.selector.metrics.ActionMetricsType
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.initiatedBy
import java.time.LocalDateTime
import java.time.ZoneId

class PersonRepository(private val database: Database, private val metricsRegistry: MicrofrontendCounter) {
    private val secureLog = KotlinLogging.logger("secureLog")
    private val log = KotlinLogging.logger { }

    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun getEnabledMicrofrontends(ident: String, innloggetnivå:Int): String = withCustomException(ident) {
        database.query {
            queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
                row.string("microfrontends")
            }.asSingle
        }?.let { Microfrontends(it).apiResponse(innloggetnivå) }
        ?: Microfrontends.emptyApiResponse()
    }

    fun enableMicrofrontend(jsonMessage: JsonMessage) {
        val ident = jsonMessage.ident
        val initiatedBy = jsonMessage.initiatedBy
        val microfrontends = getMicrofrontends(ident)
        withLogging(ident, jsonMessage.microfrontendId, "enable") {
            if (microfrontends.addMicrofrontend(jsonMessage)) {
                secureLog.info { "Oppdaterer mikrofrontend fra packet: $jsonMessage" }
                secureLog.info { "Nytt innhold er ${microfrontends.apiResponse(4)} " }
                updatePersonTable(ident, microfrontends)
                addChangelogEntry(ident, microfrontends, initiatedBy)
                metricsRegistry.countMicrofrontendActions(ActionMetricsType.ENABLE, jsonMessage.microfrontendId)
            }
        }
    }

    fun disableMicrofrontend(jsonMessage: JsonMessage) {
        val microfrontends = getMicrofrontends(jsonMessage.ident)
        withLogging(jsonMessage.ident, jsonMessage.microfrontendId, "disable") {
            if (microfrontends.removeMicrofrontend(jsonMessage.microfrontendId)) {
                updatePersonTable(jsonMessage.ident, microfrontends)
                addChangelogEntry(jsonMessage.ident, microfrontends, jsonMessage.initiatedBy)
                metricsRegistry.countMicrofrontendActions(ActionMetricsType.DISABLE, jsonMessage.microfrontendId)
            }
        }
    }

    private fun getMicrofrontends(ident: String) = database.query {
        queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
            Microfrontends(row.string("microfrontends"))
        }.asSingle
    } ?: Microfrontends()

    private fun addChangelogEntry(ident: String, microfrontends: Microfrontends, initiatedBy: String?) {
        database.update {
            queryOf(
                """INSERT INTO changelog (ident, original_data, new_data, timestamp, initiated_by) 
                    VALUES (:ident, :originalData, :newData, :now, :initiatedBy) 
                """.trimMargin(),
                mapOf(
                    "ident" to ident,
                    "originalData" to microfrontends.originalDataJsonB(),
                    "newData" to microfrontends.newDataJsonB(),
                    "now" to LocalDateTimeHelper.nowAtUtc(),
                    "initiatedBy" to initiatedBy
                )
            )
        }
    }

    private fun updatePersonTable(ident: String, microfrontends: Microfrontends) {
        database.update {
            queryOf(
                """INSERT INTO person (ident, microfrontends,created) VALUES (:ident, :newData, :now) 
                    |ON CONFLICT(ident) DO UPDATE SET microfrontends=:newData, last_changed=:now
                """.trimMargin(),
                mapOf(
                    "ident" to ident,
                    "newData" to microfrontends.newDataJsonB(),
                    "now" to LocalDateTimeHelper.nowAtUtc()
                )
            )
        }
    }

    private fun withLogging(ident: String, microfrontendId: String, operation: String, function: () -> Unit) {
        try {
            function()
        } catch (e: Exception) {
            log.error { "Feil ved $operation for mikrofrontendId med id $microfrontendId\n ${e.message}" }
            secureLog.error {
                "Feil ved $operation for mikrofrontendId med id $microfrontendId for ident $ident\n ${e.stackTraceToString()}"
            }
        }
    }
    private fun <T> withCustomException(ident: String, function: () -> T) =
        try {
            function()
        } catch (e: Exception) {
            throw DatabaseException(ident, e)
        }
}

class DatabaseException(val ident: String, val originalException: Exception) : Exception()

