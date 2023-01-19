package no.nav.tms.mikrofrontend.selector.database

import kotliquery.queryOf
import no.nav.tms.mikrofrontend.selector.metrics.ActionMetricsType
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import java.time.LocalDateTime
import java.time.ZoneId


class PersonRepository(private val database: Database, private val metricsRegistry: MicrofrontendCounter) {

    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun getEnabledMicrofrontends(ident: String): String = database.query {
        queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
            row.string("microfrontends")
        }.asSingle
    } ?: Microfrontends.emptyList()

    fun enableMicrofrontend(ident: String, microfrontendId: String) {
        val microfrontends = getMicrofrontends(ident)

        if (microfrontends.addMicrofrontendId(microfrontendId)) {
            updatePersonTable(ident, microfrontends)
            addChangelogEntry(ident, microfrontends)
            metricsRegistry.countMicrofrontendEnabled(ActionMetricsType.ENABLE, microfrontendId)
        }
    }

    fun disableMicrofrontend(ident: String, microfrontendId: String) {
        val microfrontends = getMicrofrontends(ident)

        if (microfrontends.removeMicrofrontendId(microfrontendId)) {
            updatePersonTable(ident, microfrontends)
            addChangelogEntry(ident, microfrontends)
            metricsRegistry.countMicrofrontendEnabled(ActionMetricsType.DISABLE, microfrontendId)
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
}