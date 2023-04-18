package no.nav.tms.mikrofrontend.selector.database

import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.mikrofrontend.selector.ident
import no.nav.tms.mikrofrontend.selector.metrics.ActionMetricsType
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.microfrontendId
import java.time.LocalDateTime
import java.time.ZoneId


class PersonRepository(private val database: Database, private val metricsRegistry: MicrofrontendCounter) {
    private val secureLog = KotlinLogging.logger("secureLog")
    private val log = KotlinLogging.logger { }

    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun getEnabledMicrofrontends(ident: String): String = withCustomException(ident) {
        database.query {
            queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
                row.string("microfrontends")
            }.asSingle
        } ?: Microfrontends.emptyList()
    }

    fun enableMicrofrontend(ident: String, microfrontendId: String) {
        val microfrontends = getMicrofrontends(ident)
        withLogging(ident, microfrontendId, "enable") {
            if (microfrontends.addMicrofrontend(microfrontendId)) {
                updatePersonTable(ident, microfrontends)
                addChangelogEntry(ident, microfrontends)
                metricsRegistry.countMicrofrontendEnabled(ActionMetricsType.ENABLE, microfrontendId)
            }
        }
    }

    fun enableMicrofrontend( microfrontendData: JsonMessage) {
        val ident = microfrontendData.ident
        val microfrontends = getMicrofrontends(ident)
        withLogging(ident, microfrontendData.microfrontendId, "enable") {
            if (microfrontends.addMicrofrontend(microfrontendData)) {
                updatePersonTable(ident, microfrontends)
                addChangelogEntry(ident, microfrontends)
                metricsRegistry.countMicrofrontendEnabled(ActionMetricsType.ENABLE, microfrontendData.microfrontendId)
            }
        }
    }

    fun disableMicrofrontend(ident: String, microfrontendId: String) {
        val microfrontends = getMicrofrontends(ident)
        withLogging(ident, microfrontendId, "disable") {
            if (microfrontends.removeMicrofrontend(microfrontendId)) {
                updatePersonTable(ident, microfrontends)
                addChangelogEntry(ident, microfrontends)
                metricsRegistry.countMicrofrontendEnabled(ActionMetricsType.DISABLE, microfrontendId)
            }
        }
    }

    private fun getMicrofrontends(ident: String) = database.query {
        queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
            Microfrontends(row.string("microfrontends"))
        }.asSingle
    } ?: Microfrontends()

    private fun addChangelogEntry(ident: String, microfrontends: Microfrontends) {
        database.update {
            queryOf(
                """INSERT INTO changelog (ident, original_data, new_data, timestamp) VALUES (:ident, :originalData, :newData, :now) 
                """.trimMargin(),
                mapOf(
                    "ident" to ident,
                    "originalData" to microfrontends.originalDataJsonB(),
                    "newData" to microfrontends.newDataJsonB(),
                    "now" to LocalDateTimeHelper.nowAtUtc()
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