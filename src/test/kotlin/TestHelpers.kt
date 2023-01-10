import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun enableMessage(mikrofrontendId: String, fnr: String) = """
    {
      "@event": "enable",
      "ident": "$fnr",
      "mikrofrontend_id": "$mikrofrontendId"
    }
    """.trimIndent()

fun disableMessage(mikrofrontendId: String, fnr: String) = """
    {
      "@event": "enable",
      "ident": "$fnr",
      "mikrofrontend_id": "$mikrofrontendId"
    }
    """.trimIndent()

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}