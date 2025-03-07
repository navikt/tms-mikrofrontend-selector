package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.bodyAsNullOrJsonNode
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser
import java.net.SocketTimeoutException
import java.util.UUID
import kotlin.reflect.full.primaryConstructor

class ExternalContentFecther(
    val safUrl: String,
    val httpClient: HttpClient,
    val meldekortUrl: String,
    val pdlUrl: String,
    val digisosUrl: String,
    val pdlBehandlingsnummer: String,
    val tokenFetcher: TokenFetcher,
    val dokumentarkivUrlResolver: DokumentarkivUrlResolver
) {

    val log = KotlinLogging.logger { }

    private fun safQuery(ident: String) = """ {
        "query": "query(${'$'}ident: String!) {
            dokumentoversiktSelvbetjening(ident:${'$'}ident, tema:[]) {
                tema {
                    kode
                    navn
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

    suspend fun fetchDocumentsFromSaf(user: TokenXUser): SafResponse = withErrorHandling("SAF", "$safUrl/graphql") {
        httpClient.post {
            url("$safUrl/graphql")
            header("Authorization", "Bearer ${tokenFetcher.safToken(user)}")
            header("Content-Type", "application/json")
            setBody(safQuery(user.ident))
            timeout {
                requestTimeoutMillis = 5000
            }
        }
            .let { response ->
                if (response.status != HttpStatusCode.OK) {
                    ResponseWithErrors.createFromHttpError(response)

                } else {
                    val jsonResponse = response.bodyAsNullOrJsonNode()
                    SafResponse(
                        dokumenter = jsonResponse?.safDokument(dokumentarkivUrlResolver),
                        errors = jsonResponse?.getAll<String>("errors..message")
                    )
                }
            }
    }

    suspend fun fetchMeldekort(user: TokenXUser): MeldekortResponse = getResponseAsJsonPath(
        tokenFetcher = tokenFetcher::meldekortToken,
        user = user,
        url = "$meldekortUrl/api/person/meldekortstatus",
        tjeneste = "meldekort",
        map = { jsonPath -> MeldekortResponse(meldekortApiResponse = jsonPath) },
    )

    suspend fun fetchDigisosSakstema(user: TokenXUser): DigisosResponse = getResponseAsJsonPath(
        tokenFetcher = tokenFetcher::digisosToken,
        user = user,
        url = "$digisosUrl/minesaker/innsendte",
        tjeneste = "digisos",
        requestOptions = {
            accept(ContentType.Application.Json)
            header("Nav-Callid", UUID.randomUUID())
        },
        map = { jsonPath ->
            DigisosResponse(
                dokumenter = jsonPath.digisosDokument(dokumentarkivUrlResolver)
            )
        },
    )


    suspend fun fetchPersonOpplysninger(user: TokenXUser): PdlResponse = withErrorHandling("pdl", "$pdlUrl/graphql") {
        httpClient.post {
            url("$pdlUrl/graphql")
            header("Authorization", "Bearer ${tokenFetcher.pdlToken(user)}")
            header("Content-Type", "application/json")
            header("Behandlingsnummer", pdlBehandlingsnummer)
            setBody(HentAlder(user.ident))
        }
            .let { response ->
                if (response.status != HttpStatusCode.OK) {
                    ResponseWithErrors.createFromHttpError(response)
                } else {
                    val jsonResponse = response.bodyAsNullOrJsonNode()
                    PdlResponse(
                        fødselsdato = jsonResponse?.localDateOrNull("foedselsdato..foedselsdato"),
                        fødselsår = jsonResponse?.intOrNull("foedselsdato..foedselsaar"),
                        errors = jsonResponse?.getAll<String>("errors..message") ?: emptyList(),
                    )
                }
            }
    }

    private suspend inline fun <reified T : ResponseWithErrors> getResponseAsJsonPath(
        tokenFetcher: suspend (TokenXUser) -> String,
        user: TokenXUser,
        url: String,
        tjeneste: String,
        requestOptions: HttpRequestBuilder.() -> Unit = {},
        crossinline map: (JsonPathInterpreter) -> T,
    ): T = withErrorHandling(tjeneste, url) {
        val token = tokenFetcher(user)
        httpClient.get {
            url(url)
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            header("Nav-Consumer-Id", "min-side:tms-mikrofrontend-selector")
            requestOptions()
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                ResponseWithErrors.createFromHttpError(response)
            else
                response.bodyAsNullOrJsonNode()?.let(map)
                    ?: ResponseWithErrors.errorInJsonResponse(response.bodyAsText())
        }
    }
    private inline fun <reified T : ResponseWithErrors> withErrorHandling(
        tjeneste: String,
        url: String,
        function: () -> T
    ) =
        try {
            function()
        } catch (socketTimout: SocketTimeoutException) {
            ResponseWithErrors.createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "Sockettimeout ${errorDetails(socketTimout)}",
                className = T::class.qualifiedName ?: "unknown"
            )
        } catch (requestTimeoutException: HttpRequestTimeoutException) {
            ResponseWithErrors.createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "Requesttimeout ${errorDetails(requestTimeoutException)}",
                className = T::class.qualifiedName ?: "unknown"
            )
        } catch (tokenFetcherException: TokenFetcher.TokenFetcherException) {
            ResponseWithErrors.createWithError(
                constructor = T::class.primaryConstructor,
                errorMessage = "errors fetching token $tjeneste",
                className = T::class.qualifiedName ?: "unknown"
            )
        } catch (e: Exception) {
            throw ApiException(tjeneste, url, e)
        }
}

class ApiException(tjeneste: String, url: String, e: Exception) :
    Exception(
        """
            |Kall til ekstern tjeneste $tjeneste feiler. Url: $url ${errorDetails(e)}. 
           
        """.trimMargin()
    )


private class HentAlder(ident: String) {
    val query = """
        query(${'$'}ident: ID!) {
            hentPerson(ident: ${'$'}ident) {
                foedselsdato {
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

private class HentSafDokumenter(ident: String) {
    val query = """ 
        query(${'$'}ident: String!) {
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
