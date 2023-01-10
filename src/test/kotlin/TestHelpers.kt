import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun enableMessage(microfrontendId: String, fnr: String) = """
    {
      "@event_name": "enable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId"
    }
    """.trimIndent()

fun disableMessage(microfrontendId: String, fnr: String) = """
    {
      "@event_name": "disable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId"
    }
    """.trimIndent()

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}