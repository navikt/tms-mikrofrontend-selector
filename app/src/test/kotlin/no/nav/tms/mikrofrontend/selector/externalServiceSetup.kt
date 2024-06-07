package no.nav.tms.mikrofrontend.selector


import com.nfeld.jsonpathkt.extension.read
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.tms.common.testutils.GraphQlRouteProvider
import no.nav.tms.common.testutils.RouteProvider
import no.nav.tms.common.testutils.assert
import java.time.LocalDateTime


const val testHost = "http://test.nav.no"

class SafRoute(
    sakstemaer: List<String> = emptyList(),
    errorMsg: String? = null,
    ident: String = "12345678910"
) : GraphQlRouteProvider(errorMsg = errorMsg, path = "graphql", assert = { call ->
    val callBody = call.receiveText().let { objectMapper.readTree(it) }
    callBody.read<String>("$.query").assert {
        this shouldNotBe null
        this shouldBe "query(${'$'}ident: String!) { dokumentoversiktSelvbetjening(ident:${'$'}ident, tema:[]) { tema { kode journalposter{ relevanteDatoer { dato } } } } }"
    }
    callBody.read<String>("$.variables.ident") shouldBe ident
}) {

    override val data: String = """{
            "dokumentoversiktSelvbetjening": {
              "tema": ${
        sakstemaer.joinToString(prefix = "[", postfix = "]") {
            """ { "kode": "$it",  
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
    override fun content(): String = ovverideContent ?: """
        {
          "underOppfolging": $underOppfølging
        }
    """.trimIndent()

}
class DigisosRoute(private val hasSosialhjelp: Boolean = false) :
    RouteProvider(path = "minesaker/innsendte", routeMethodFunction = Routing::get) {
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
