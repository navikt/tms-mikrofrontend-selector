package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.postgresql.util.PGobject

internal class Microfrontends(stringlist: String? = null) {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val originalData: List<String>? =
        stringlist?.let { objectMapper.readTree(it)["microfrontends"] }?.toList()?.map { it.asText() }
    private val newData = originalData?.toMutableSet() ?: mutableSetOf()

    companion object {
        fun emptyList(): String = """{ "microfrontends":[] }"""
    }


    fun addMicrofrontendId(microfrontendId: String) = newData.add(microfrontendId)
    fun removeMicrofrontendId(microfrontendId: String) = newData.remove(microfrontendId)

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

}
