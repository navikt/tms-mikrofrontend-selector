package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.databind.JsonNode
import no.nav.tms.mikrofrontend.selector.config.Database
import java.time.LocalDateTime
import java.time.ZoneId

class MicrofrontendRepository(private val database: Database) {
    object LocalDateTimeHelper {
        fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun enable(fnr: String, mikrofrontendId: String) {
        TODO("Enable not yet implemented")
    }
    fun disable(){
        TODO("Disabled not yet implemented")

    }
    fun getEnabledMicrofrontends(fnr: String): JsonNode {
        TODO(" getEnabledMicrofrontends Not yet implemented")
    }
}