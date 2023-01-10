package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import no.nav.tms.mikrofrontend.selector.config.Database
import org.intellij.lang.annotations.Language
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.time.ZoneId


private val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

class PersonRepository(private val database: Database) {

    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun enableMicrofrontend(ident: String, microfrontendId: String) {
        val microfrontends = database.query {
            queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
                Microfrontends(row.string("microfrontends"))
            }.asSingle
        } ?: Microfrontends()

        if (microfrontends.updateData(microfrontendId)) {
            updatePersonTable(ident, microfrontends)
            addChangelogEntry(ident, microfrontends)
        }
    }


    fun disableMicrofrontend() {
        TODO("Disabled not yet implemented")

    }

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

    fun getEnabledMicrofrontends(ident: String): String? = database.query {
        queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
            row.string("microfrontends")
        }.asSingle
    } ?: Microfrontends.emptyJsonResponse()
}

private class Microfrontends(stringlist: String? = null) {
    val originalData: List<String>? =
        stringlist?.let { objectMapper.readTree(it)["microfrontends"] }?.toList()?.map { it.asText() }
    val newData = originalData?.toMutableList() ?: mutableSetOf()

    fun updateData(microfrontendId: String) = newData.add(microfrontendId)

    @Language("JSON")
    fun newDataJsonB(): PGobject = """{ "microfrontends": ${newData.jsonArrayString()}}""".trimMargin().jsonB()
    fun originalDataJsonB(): PGobject? =
        originalData?.let { """{ "microfrontends": ${it.jsonArrayString()}}""".trimMargin() }?.jsonB()

    private fun <E> Collection<E>.jsonArrayString(): String = joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
        transform = { """"$it"""" }
    )

    private fun String.jsonB() = PGobject().apply {
        type = "jsonb"
        value = this@jsonB
    }

    companion object {
        fun emptyJsonResponse(): String = """{ "microfrontends":[] }"""
    }
}
