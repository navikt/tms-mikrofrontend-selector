package no.nav.tms.mikrofrontend.selector

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.delay


const val testHost = "http://test.nav.no"

abstract class RouteProvider(
    val path: String,
    private val routeMethodFunction: Routing.(
        path: String,
        suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
    ) -> Unit,
    private val statusCode: HttpStatusCode = OK,
) {
    abstract fun content(): String
    fun Routing.initRoute() {
        routeMethodFunction(path) {
            delay(1000)
            call.respondText(
                contentType = ContentType.Application.Json,
                status = statusCode,
                provider = ::content
            )
        }
    }
}

class SafRoute(
    private val sakstemaer: List<String> = emptyList(),
    errorMsg: String? = null
) : RouteProvider(path = "graphql", Routing::post) {
    private val errors = errorMsg?.let {
        """
                  "errors": [
                                  {
                                    "message": "$it",
                                    "locations": [
                                      {
                                        "line": 2,
                                        "column": 3
                                      }
                                    ],
                                    "path": [
                                      "journalpost"
                                    ],
                                    "extensions": {
                                      "code": "not_found",
                                      "classification": "ExecutionAborted"
                                    }
                                  }
                                ],  
                """.trimIndent()
    } ?: ""
    override fun content(): String = """
        {
          $errors  
          "data": {
            "dokumentoversiktSelvbetjening": {
              "tema": ${
        sakstemaer.joinToString(prefix = "[", postfix = "]") { """{ "kode": "$it" }""".trimIndent() }
    }
            }
          }
        }
    ""${'"'}.trimIndent()
""".trimIndent()
}

class MeldekortRoute : RouteProvider(path = "api/person/meldekortstatus", routeMethodFunction = Routing::get) {

    //TODO
    override fun content(): String {
        return """
            {
              "meldekort": 0,
              "etterregistrerteMeldekort": 0,
              "antallGjenstaaendeFeriedager": 0,
              "nesteMeldekort": null,
              "nesteInnsendingAvMeldekort": null
            }
        """.trimIndent()
    }
}

class OppfolgingRoute(private val underOppfølging: Boolean = false) :
    RouteProvider(path = "api/niva3/underoppfolging", routeMethodFunction = Routing::get) {
    //TODO: sjekk route i proxy
    override fun content(): String = """
        {
          "underOppfolging": $underOppfølging
        }
    """.trimIndent()

}

class ArbeidsøkerRoute(private val erArbeidsøker: Boolean = false, private val erStandard: Boolean = false) :
    RouteProvider(path = "aia-backend/er-arbeidssoker", routeMethodFunction = Routing::get) {
    override fun content(): String = """
        {
          "erArbeidssoker": $erArbeidsøker,
          "erStandard": $erStandard
        }
    """.trimIndent()

}


fun ApplicationTestBuilder.initExternalServices(
    vararg routeProviders: RouteProvider
) = externalServices {
    hosts(testHost) {
        routing {
            routeProviders.forEach { provider ->
                provider.run { this@routing.initRoute() }
            }
        }
    }
}
