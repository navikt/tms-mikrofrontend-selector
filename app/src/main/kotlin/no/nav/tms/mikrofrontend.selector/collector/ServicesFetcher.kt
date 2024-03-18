package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.tms.mikrofrontend.selector.collector.NullOrJsonNode.Companion.bodyAsNullOrJsonNode
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class ServicesFetcher(
    val safUrl: String,
    val safClientId: String,
    val httpClient: HttpClient,
    val tokendingsService: TokendingsService,
    val oppfølgingBaseUrl: String,
    val oppfølgingClientId: String,
    val aiaBackendUrl: String,
    val aiaBackendClientId: String,
    val meldekortUrl: String,
    val meldekortClientId: String,
) {

    val log = KotlinLogging.logger { }
    fun query(ident: String) = """ {
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
    """.compactJson()

    private fun String.compactJson(): String =
        trimIndent()
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")


    suspend fun fetchSakstema(user: TokenXUser): SafResponse = withErrorHandling {
        httpClient.post {
            url("$safUrl/graphql")
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, safClientId)}")
            header("Content-Type", "application/json")
            setBody(query(user.ident))
        }
            .let { response ->
                if (response.status != HttpStatusCode.OK) {
                    SafResponse(response = response)
                } else {
                    val jsonResponse = response.bodyAsNullOrJsonNode()
                    SafResponse(
                        sakstemakoder = jsonResponse?.list<String>("data.dokumentoversiktSelvbetjening.tema..kode"),
                        errors = jsonResponse?.list<String>("errors..message")
                    )
                }
            }
    }

    suspend fun fetchOppfolging(user: TokenXUser): OppfolgingResponse = withErrorHandling {
        httpClient.get("$oppfølgingBaseUrl/api/niva3/underoppfolging") {
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, oppfølgingClientId)}")
            header("Content-Type", "application/json")
            //BODY?
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                OppfolgingResponse(response = response)
            else
                OppfolgingResponse(
                    underOppfolging = response.bodyAsNullOrJsonNode()?.boolean("underOppfolging")
                )
        }
    }


    suspend fun fetchArbeidsøker(user: TokenXUser): ArbeidsøkerResponse = withErrorHandling {
        httpClient.get("$aiaBackendUrl/aia-backend/er-arbeidssoker") {
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, aiaBackendClientId)}")
            header("Content-Type", "application/json")
            //TODO: body
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                ArbeidsøkerResponse(response = response)
            else
                response.bodyAsNullOrJsonNode().let { jsonNode ->
                    ArbeidsøkerResponse(
                        erArbeidssoker = jsonNode?.boolean("erArbeidssoker") ?: false,
                        erStandard = jsonNode?.boolean("erStandard") ?: false
                    )
                }

        }
    }

    suspend fun fetchMeldekort(user: TokenXUser): MeldekortResponse = withErrorHandling {
        httpClient.get("$meldekortUrl/api/person/meldekortstatus") {
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, meldekortClientId)}")
            header("Content-Type", "application/json")
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                MeldekortResponse(response = response, errors = "Kall til meldekortstatus feiler med ${response.status}")
            else
                response.bodyAsNullOrJsonNode().let {
                    MeldekortResponse(meldekortApiResponse = response.bodyAsNullOrJsonNode())
                }

        }
    }
    private suspend fun <T> withErrorHandling(function: suspend () -> T) =
        try {
            function()
        } catch (e: Exception) {
            throw ApiException(e)
        }

    class ApiException(e: Exception) :
        Exception("Kall til eksterne tjenester feiler: ${e.message ?: e::class.simpleName}")
}
