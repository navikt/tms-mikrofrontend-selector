import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf

fun enableMessage(microfrontendId: String, fnr: String, sikkerhetsnivå:Int=4) = """
    {
      "@action": "enable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId",
      "sikkerhetsnivå" : $sikkerhetsnivå
    }
    """.trimIndent()

fun disableMessage(microfrontendId: String, fnr: String) = """
    {
      "@action": "disable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId"
    }
    """.trimIndent()

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

