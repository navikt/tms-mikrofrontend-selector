package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nfeld.jsonpathkt.extension.read
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime


const val testHost = "http://test.nav.no"

abstract class RouteProvider(
    val path: String,
    private val routeMethodFunction: Routing.(
        path: String,
        suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
    ) -> Unit,
    private val statusCode: HttpStatusCode = OK,
    private val assert: suspend (ApplicationCall) -> Unit = {}
) {
    abstract fun content(): String
    fun Routing.initRoute() {
        routeMethodFunction(path) {
            assert(call)
            delay(1000)
            call.respondText(
                contentType = ContentType.Application.Json,
                status = statusCode,
                provider = ::content
            )
        }
    }
}

abstract class GraphQlRouteProvider(
    errorMsg: String?,
    path: String,
    statusCode: HttpStatusCode = OK,
    assert: suspend (ApplicationCall) -> Unit = {},
) : RouteProvider(path, Routing::post, statusCode, assert) {
    val errors = errorMsg?.let {
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
    abstract val data: String
    override fun content(): String = """
        {
        $errors
        "data": $data
        }
    """.trimIndent()
}

class SafRoute(
    sakstemaer: List<String> = emptyList(),
    errorMsg: String? = null,
    ident: String
) : GraphQlRouteProvider(errorMsg = errorMsg, path = "graphql", assert = {
    val safQuery: String =""" {
    "query": "query(${'$'}ident: String!) {
    dokumentoversiktSelvbetjening(ident:${'$'}ident, tema:[]) {
    tema {
    kode
    journalposter{
    relevanteDatoer {
    dato
    }
    }
    }
    }
    }",
    "variables": {"ident" : "$ident"}
    }
    """.trimIndent()
    val callBody = it.receiveText().let { objectMapper.readTree(it) }
    callBody.read<String>("$.query") shouldNotBe null
    callBody.read<String>("$.variables.ident") shouldBe ident
}) {

    override val data: String = """{
            "dokumentoversiktSelvbetjening": {
              "tema": ${
        sakstemaer.joinToString(prefix = "[", postfix = "]") {
            """ { "kode": "$it",  
                        "journalposter": {
                            "relevanteDatoer": {
                                "dato": "${LocalDateTime.now()}"
                            }
                        }
                      }""".trimIndent()
        }
    }
            }
          }""".trimIndent()
}

class MeldekortRoute(private val harMeldekort: Boolean = false, httpStatusCode: HttpStatusCode = OK) :
    RouteProvider(
        path = "api/person/meldekortstatus",
        routeMethodFunction = Routing::get,
        statusCode = httpStatusCode
    ) {
    override fun content(): String = if (harMeldekort)
        """{
          "antallGjenstaaendeFeriedager": 0,
          "etterregistrerteMeldekort": 2,
          "meldekort": 2,
          "nesteInnsendingAvMeldekort": "2019-09-30",
          "nesteMeldekort": {
            "fra": "2019-09-09",
            "kanSendesFra": "2019-09-21",
            "til": "2024-09-22",
            "uke": "37-38"
          }
        }""".trimIndent()
    else """
            {
              "meldekort": 0,
              "etterregistrerteMeldekort": 0,
              "antallGjenstaaendeFeriedager": 0,
              "nesteMeldekort": null,
              "nesteInnsendingAvMeldekort": null
            }
        """.trimIndent()
}


class OppfolgingRoute(private val underOppfølging: Boolean = false, val ovverideContent: String? = null) :
    RouteProvider(path = "api/niva3/underoppfolging", routeMethodFunction = Routing::get) {
    //TODO: sjekk route i proxy
    override fun content(): String = ovverideContent ?: """
        {
          "underOppfolging": $underOppfølging
        }
    """.trimIndent()

}

class ArbeidsøkerRoute(
    private val erArbeidsøker: Boolean = false,
    private val erStandard: Boolean = false,
    val ovverideContent: String? = null,
    private val brukNyAia: Boolean = false
) :
    RouteProvider(path = "aia-backend/er-arbeidssoker", routeMethodFunction = Routing::get) {
    override fun content(): String = ovverideContent ?: """
        {
          "erArbeidssoker": $erArbeidsøker,
          "erStandard": $erStandard,
          "brukNyAia": $brukNyAia
        }
    """.trimIndent()
}

class PdlRoute(
    fødselsdato: String = "1978-05-05",
    fødselssår: Int = 1978,
    errorMsg: String? = null
) :
    GraphQlRouteProvider(errorMsg = errorMsg, path = "pdl/graphql") {
    override val data: String = if (errorMsg == null) """
         {
           "hentPerson": {
      "foedsel": [
        {
          "foedselsdato": "$fødselsdato",
          "foedselsaar": $fødselssår
        }
      ]
    }
         }
    """.trimIndent() else "{}"
}


fun ApplicationTestBuilder.initExternalServices(
    vararg routeProviders: RouteProvider
) = externalServices {
    hosts(testHost) {
        routing {
            routeProviders.forEach { provider ->
                provider.run {
                    this@routing.initRoute()
                }
            }
        }
    }
}
