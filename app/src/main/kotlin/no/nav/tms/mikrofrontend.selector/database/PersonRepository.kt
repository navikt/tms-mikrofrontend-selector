package no.nav.tms.mikrofrontend.selector.database

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.mikrofrontend.selector.metrics.ActionMetricsType
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.initiatedBy
import java.time.LocalDateTime
import java.time.ZoneId

class PersonRepository(private val database: Database, private val counter: MicrofrontendCounter) {
    private val log = KotlinLogging.logger { }

    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    internal fun getEnabledMicrofrontends(ident: String): Microfrontends? = withCustomException(ident) {
        database.query {
            queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
                row.string("microfrontends")
            }.asSingle
        }?.let { Microfrontends(it) }
    }

    fun enableMicrofrontend(jsonMessage: JsonMessage) {
        val ident = jsonMessage.ident
        val microfrontends = getMicrofrontends(ident)
        if (microfrontends.addMicrofrontend(jsonMessage)) {
            log.info { "Oppdaterer/enabler mikrofrontend" }
            updatePersonTable(ident, microfrontends)
            addChangelogEntry(ident, microfrontends, jsonMessage.initiatedBy)
            counter.countMicrofrontendActions(ActionMetricsType.ENABLE, jsonMessage.microfrontendId)
        }
    }


    fun disableMicrofrontend(jsonMessage: JsonMessage) {
        val microfrontends = getMicrofrontends(jsonMessage.ident)
        if (microfrontends.removeMicrofrontend(jsonMessage.microfrontendId)) {
            log.info { "Disabler mikrofrontend }" }
            updatePersonTable(jsonMessage.ident, microfrontends)
            addChangelogEntry(jsonMessage.ident, microfrontends, jsonMessage.initiatedBy)
            counter.countMicrofrontendActions(ActionMetricsType.DISABLE, jsonMessage.microfrontendId)
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

    private fun <T> withCustomException(ident: String, function: () -> T) =
        try {
            function()
        } catch (e: Exception) {
            throw DatabaseException(ident, e)
        }
}

class DatabaseException(val ident: String, val originalException: Exception) : Exception()

