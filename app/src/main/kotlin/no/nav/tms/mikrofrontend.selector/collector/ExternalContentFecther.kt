package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.bodyAsNullOrJsonNode
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class ExternalContentFecther(
    val safUrl: String,
    val httpClient: HttpClient,
    val oppfølgingBaseUrl: String,
    val aiaBackendUrl: String,
    val meldekortUrl: String,
    val pdlUrl: String,
    val tokenFetcher: TokenFetcher
) {

    val log = KotlinLogging.logger { }

    private fun safQuery(ident: String) = """ {
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

    suspend fun fetchSakstema(user: TokenXUser): SafResponse = withErrorHandling {
        httpClient.post {
            url("$safUrl/graphql")
            header("Authorization", "Bearer ${tokenFetcher.safToken(user)}")
            header("Content-Type", "application/json")
            setBody(safQuery(user.ident))
        }
            .let { response ->
                if (response.status != HttpStatusCode.OK) {
                    ResponseWithErrors.createFromHttpError(response, SafResponse::class)

                } else {
                    val jsonResponse = response.bodyAsNullOrJsonNode()
                    SafResponse(
                        sakstemakoder = jsonResponse?.getAll<String>("data.dokumentoversiktSelvbetjening.tema..kode"),
                        errors = jsonResponse?.getAll<String>("errors..message")
                    )
                }
            }
    }

    suspend fun fetchOppfolging(user: TokenXUser): OppfolgingResponse = getResponseAsJsonPath(
        token = tokenFetcher.oppfolgingToken(user),
        url = "$oppfølgingBaseUrl/api/niva3/underoppfolging",
        map = { OppfolgingResponse(underOppfolging = it.boolean("underOppfolging")) }
    )


    suspend fun fetchArbeidsøker(user: TokenXUser): ArbeidsøkerResponse = getResponseAsJsonPath(
        token = tokenFetcher.aiaToken(user),
        url = "$aiaBackendUrl/aia-backend/er-arbeidssoker",
        map = { jsonPath ->
            ArbeidsøkerResponse(
                erArbeidssoker = jsonPath.boolean("erArbeidssoker"),
                erStandard = jsonPath.boolean("erStandard")
            )
        }
    )

    suspend fun fetchMeldekort(user: TokenXUser): MeldekortResponse = getResponseAsJsonPath(
        token = tokenFetcher.meldekortToken(user),
        url = "$meldekortUrl/api/person/meldekortstatus",
        map = { jsonPath -> MeldekortResponse(meldekortApiResponse = jsonPath) }
    )

    suspend fun fetchPersonOpplysninger(user: TokenXUser): PdlResponse = withErrorHandling {
        httpClient.post {
            url("$pdlUrl/graphql")
            header("Authorization", "Bearer ${tokenFetcher.safToken(user)}")
            header("Content-Type", "application/json")
            setBody(HentAlder(user.ident))
        }
            .let { response ->
                if (response.status != HttpStatusCode.OK) {
                    ResponseWithErrors.createFromHttpError(response, PdlResponse::class)
                } else {
                    val jsonResponse = response.bodyAsNullOrJsonNode()
                    PdlResponse(
                        fødselsdato = jsonResponse?.localDateOrNull("data.hentPerson.foedsel.foedselsdato"),
                        fødselsår = jsonResponse?.int("data.hentPerson.foedsel.foedselsaar")
                            ?: 0, //TODO fiks guaranteed jsonResponse
                        errors = jsonResponse?.listOrNull<String>("errors..message") ?: emptyList(),
                    )
                }
            }
    }

    private suspend fun <T> withErrorHandling(function: suspend () -> T) =
        try {
            function()
        } catch (e: Exception) {
            throw ApiException(e)
        }


    private suspend inline fun <reified T : ResponseWithErrors> getResponseAsJsonPath(
        token: String,
        url: String,
        crossinline map: (JsonPathInterpreter) -> T
    ): T = try {
        httpClient.get {
            url(url)
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            header("Nav-Consumer-Id", "min-side:tms-mikrofrontend-selector")
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                ResponseWithErrors.createFromHttpError(response, T::class)
            else
                response.bodyAsNullOrJsonNode()?.let(map)
                    ?: ResponseWithErrors.errorInJsonResponse(response.bodyAsText(), T::class)
        }
    } catch (e: Exception) {
        throw ApiException(e)
    }


    class ApiException(e: Exception) :
        Exception(
            """
            |Kall til eksterne tjenester feiler: ${errorDetails(e)}. 
           
        """.trimMargin()
        )
}

private class HentAlder(ident: String) {
    val query = """
        query(${'$'}ident: ID!) {
            hentPerson(ident: ${'$'}ident) {
                foedsel {
                    foedselsdato,
                    foedselsaar,
                }
            }
        }
    """.compactJson()

    val variables = mapOf(
        "ident" to ident
    )
}

private fun String.compactJson(): String =
    trimIndent()
        .replace("\r", " ")
        .replace("\n", " ")
        .replace("\\s+".toRegex(), " ")