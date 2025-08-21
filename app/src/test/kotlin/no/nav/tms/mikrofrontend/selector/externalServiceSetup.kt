package no.nav.tms.mikrofrontend.selector


import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.LocalDateTime


const val testHost = "http://test.nav.no"

class SafRoute(
    sakstemaer: List<String> = emptyList(),
    val errorMsg: String? = null
) : RouteProvider(path = "graphql", method = HttpMethod.Post) {

    private val data = """{
            "dokumentoversiktSelvbetjening": {
              "tema": ${
        sakstemaer.joinToString(prefix = "[", postfix = "]") {
            """ { "kode": "$it",
                        "navn": "dummyNavn",
                        "journalposter": [{
                            "relevanteDatoer": [ {
                            
                                "dato": "${LocalDateTime.now()}"
                            },
                            {
                            
                                "dato": "${LocalDateTime.now().minusMinutes(4)}"
                            }
                           ]
                        }]
                      }""".trimIndent()
        }
    }
            }
          }""".trimIndent()

    override fun content() = if (errorMsg == null) {
        """
            {
                "data": $data
            }
        """.trimIndent()
    } else {
        """
            {
                "errors": [
                    {
                        "message": "$errorMsg"
                    }
                ]
            }
        """.trimIndent()
    }
}

class MeldekortApiRoute(private val harMeldekort: Boolean = false, httpStatusCode: HttpStatusCode = OK) :
    RouteProvider(
        path = "api/person/meldekortstatus",
        method = HttpMethod.Get,
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

class DpMeldekortRoute(private val harMeldekort: Boolean = false, httpStatusCode: HttpStatusCode = OK) :
    RouteProvider(
        path = "meldekortstatus",
        method = HttpMethod.Get,
        statusCode = httpStatusCode
    ) {
    override fun content(): String = if (harMeldekort)
        """{
          "antallGjenstaaendeFeriedager": 0,
          "etterregistrerteMeldekort": 3,
          "meldekort": 3,
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

class DigisosRoute(private val hasSosialhjelp: Boolean = false) :
    RouteProvider(path = "minesaker/innsendte", method = HttpMethod.Get) {
    override fun content(): String = if (hasSosialhjelp) {
        """ [
                {
                 "navn":"Økonomisk sosialhjelp",
                 "kode":"KOM",
                 "sistEndret": "${LocalDateTime.now().minusWeeks(1)}"
                }
            ]
        """.trimMargin()
    } else "[]"


}

class PdlRoute(
    fødselsdato: String = "1978-05-05",
    fødselssår: Int = 1978,
    val errorMsg: String? = null
) :
    RouteProvider(path = "pdl/graphql", HttpMethod.Post) {

    private val data: String = """
         {
           "hentPerson": {
            "foedselsdato": [
                {
                    "foedselsdato": "$fødselsdato",
                    "foedselsaar": $fødselssår
                }
            ]
          }
        }
    """

    override fun content() = if (errorMsg == null) {
        """
            {
                "data": $data
            }
        """.trimIndent()
    } else {
        """
            {
                "errors": [
                    {
                        "message": "$errorMsg"
                    }
                ]
            }
        """.trimIndent()
    }
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

abstract class RouteProvider(
    val path: String,
    val method: HttpMethod,
    val statusCode: HttpStatusCode = OK,
) {
    abstract fun content(): String

    fun Route.initRoute() {
        route(path, method) {
            handle {
                call.respondText(content(), status = statusCode, contentType = ContentType.Application.Json)
            }
        }
    }
}
