package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.bodyAsNullOrJsonNode
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import java.net.SocketTimeoutException
import java.util.UUID
import kotlin.reflect.full.primaryConstructor

class ExternalContentFetcher(
    private val httpClient: HttpClient,
    private val meldekortApiUrl: String,
    private val dpMeldekortUrl: String,
    private val digisosUrl: String,
    private val pdlConsumer: PdlConsumer,
    private val safConsumer: SafConsumer,
    private val tokenFetcher: TokenFetcher,
    private val dokumentarkivUrlResolver: DokumentarkivUrlResolver
) {
    suspend fun fetchDocumentsFromSaf(user: UserPrincipal): SafResponse = withErrorHandling("SAF", "") {
        return safConsumer.hentTemaer(user.ident, tokenFetcher.safToken(user))
    }

    suspend fun fetchFellesMeldekort(user: UserPrincipal): MeldekortResponse = getResponseAsJsonPath(
        tokenFetcher = tokenFetcher::meldekortApiToken,
        user = user,
        url = "$meldekortApiUrl/api/person/meldekortstatus",
        tjeneste = "meldekortApi",
        map = { jsonPath -> MeldekortResponse(meldekortResponse = jsonPath) },
    )

    suspend fun fetchDpMeldekort(user: UserPrincipal): MeldekortResponse = getResponseAsJsonPath(
        tokenFetcher = tokenFetcher::dpMeldekortToken,
        user = user,
        url = "$dpMeldekortUrl/meldekortstatus",
        tjeneste = "dpMeldekort",
        map = { jsonPath -> MeldekortResponse(meldekortResponse = jsonPath) },
        errorHandlerOverride = { response ->
            // dp-meldekortregister sender 404 når de ikke har noen meldekort på bruker
            if(response.status == HttpStatusCode.NotFound) {
                MeldekortResponse()
            } else {
                null
            }
        }
    )

    suspend fun fetchDigisosSakstema(user: UserPrincipal): DigisosResponse = getResponseAsJsonPath(
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

    suspend fun fetchFoedselsdato(user: UserPrincipal): PdlResponse = withErrorHandling("pdl", "") {
        pdlConsumer.hentFoedselsdato(user.ident, tokenFetcher.pdlToken(user))
    }

    private suspend inline fun <reified T : ResponseWithErrors> getResponseAsJsonPath(
        tokenFetcher: suspend (UserPrincipal) -> String,
        user: UserPrincipal,
        url: String,
        tjeneste: String,
        requestOptions: HttpRequestBuilder.() -> Unit = {},
        errorHandlerOverride: (HttpResponse) -> T? = { null },
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

            if (response.status == HttpStatusCode.OK) {
                response.bodyAsNullOrJsonNode()?.let(map)
                    ?: ResponseWithErrors.errorInJsonResponse()
            } else when(val override = errorHandlerOverride(response)) {
                null -> ResponseWithErrors.createFromHttpError(response)
                else -> override
            }
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
    Exception("Kall til ekstern tjeneste $tjeneste feiler. Url: $url ${errorDetails(e)}.")
