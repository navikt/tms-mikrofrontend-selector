package no.nav.tms.mikrofrontend.selector.database

import no.nav.tms.mikrofrontend.selector.config.Database
import java.time.LocalDateTime
import java.time.ZoneId

class MikrofrontendRepository(private val database: Database) {
    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun enable(){}
    fun disable(){}
}