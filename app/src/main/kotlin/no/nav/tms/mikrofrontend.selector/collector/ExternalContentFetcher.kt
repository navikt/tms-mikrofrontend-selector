package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import kotlin.reflect.full.primaryConstructor

class ExternalContentFetcher(
    private val httpClient: HttpClient,
    private val meldekortApiUrl: String,
    private val dpMeldekortUrl: String,
    private val digisosUrl: String,
    private val pdlConsumer: PdlConsumer,
    private val safConsumer: SafConsumer,
    private val tokenFetcher: TokenFetcher,
    private val sosialHjelpInnsynUrl: String
) {
    suspend fun fetchDocumentsFromSaf(user: UserPrincipal): SafResponse = withErrorHandling("SAF", "") {
        return safConsumer.hentTemaer(user.ident, tokenFetcher.safToken(user))
    }

    suspend fun fetchFellesMeldekort(user: UserPrincipal): MeldekortResponse = getMeldekort(
        tokenSupplier = { tokenFetcher.meldekortApiToken(user) },
        url = "$meldekortApiUrl/api/person/meldekortstatus",
        tjeneste = "meldekortApi",
    )

    suspend fun fetchDpMeldekort(user: UserPrincipal): MeldekortResponse = getMeldekort(
        tokenSupplier = { tokenFetcher.dpMeldekortToken(user) },
        url = "$dpMeldekortUrl/meldekortstatus",
        tjeneste = "dpMeldekort"
    )

    suspend fun fetchDigisosSakstema(user: UserPrincipal): DigisosResponse = getDigisosSakstema(
        tokenSupplier = { tokenFetcher.digisosToken(user) },
        url = "$digisosUrl/minesaker/innsendte"
    )

    suspend fun fetchFoedselsdato(user: UserPrincipal): PdlResponse = withErrorHandling("pdl", "") {
        pdlConsumer.hentFoedselsdato(user.ident, tokenFetcher.pdlToken(user))
    }

    private suspend fun getDigisosSakstema(
        tokenSupplier: suspend () -> String,
        url: String
    ): DigisosResponse = withErrorHandling("digisos", url) {
        httpClient.get {
            url(url)
            header(HttpHeaders.Authorization, "Bearer ${tokenSupplier()}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header("Nav-Consumer-Id", "min-side:tms-mikrofrontend-selector")
        }.let { response ->

            when (response.status) {
                HttpStatusCode.OK -> {
                    val response: List<DigisosInnsendtSoknadDto> = response.body()

                    val temaer = response.map {
                        Tema(
                            kode = it.kode,
                            navn = it.navn,
                            sistEndret = LocalDateTime.parse(it.sistEndret),
                            url = sosialHjelpInnsynUrl
                        )
                    }

                    DigisosResponse(temaer)
                }
                else -> ResponseWithErrors.createFromHttpError(response)
            }
        }
    }

    private data class DigisosInnsendtSoknadDto(
        val navn: String,
        val kode: String,
        val sistEndret: String,
    )

    private suspend fun getMeldekort(
        tokenSupplier: suspend () -> String,
        url: String,
        tjeneste: String
    ): MeldekortResponse = withErrorHandling(tjeneste, url) {
        httpClient.get {
            url(url)
            header(HttpHeaders.Authorization, "Bearer ${tokenSupplier()}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header("Nav-Consumer-Id", "min-side:tms-mikrofrontend-selector")
        }.let { response ->

            when (response.status) {
                HttpStatusCode.OK -> {
                    val response: MeldekortDto = response.body()

                    MeldekortResponse(response.meldekortTilUtfylling.isNotEmpty())
                }
                HttpStatusCode.NotFound -> MeldekortResponse(harMeldekort = false)
                else -> ResponseWithErrors.createFromHttpError(response)
            }
        }
    }

    private data class MeldekortDto(
        val meldekortTilUtfylling: List<Any> = emptyList()
    )

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
