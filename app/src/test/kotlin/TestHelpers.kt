import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun enableMessage(microfrontendId: String, fnr: String, sikkerhetsnivå: Int = 4, initiatedBy: String? ="default-team") =
    """
    {
      "@action": "enable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId",
      "sikkerhetsnivå" : $sikkerhetsnivå
      ${initiatedBy?.let { """ ,"initiated_by": "$initiatedBy" """ }?:""}
    }
    """.trimIndent()

fun disableMessage(microfrontendId: String, fnr: String, initiatedBy: String = "default-team") = """
    {
      "@action": "disable",
      "ident": "$fnr",
      "microfrontend_id": "$microfrontendId",
      "initiated_by": "$initiatedBy"
}
    """.trimIndent()

internal val objectMapper = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
}

