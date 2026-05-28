package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.*
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import java.net.SocketTimeoutException
import java.time.LocalDateTime

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
    suspend fun fetchDocumentsFromSaf(user: UserPrincipal): ExternalResponse<Temaliste> = withErrorHandling(
        tjeneste = ExternalService.Saf,
        default = Temaliste(emptyList())
    ) {
        safConsumer.hentTemaer(user.ident, tokenFetcher.safToken(user))
    }

    suspend fun fetchFellesMeldekort(user: UserPrincipal): ExternalResponse<MeldekortStatus> = getMeldekort(
        tokenSupplier = { tokenFetcher.meldekortApiToken(user) },
        url = "$meldekortApiUrl/api/person/meldekortstatus",
        tjeneste = ExternalService.MeldekortApi
    )

    suspend fun fetchDpMeldekort(user: UserPrincipal): ExternalResponse<MeldekortStatus> = getMeldekort(
        tokenSupplier = { tokenFetcher.dpMeldekortToken(user) },
        url = "$dpMeldekortUrl/meldekortstatus",
        tjeneste = ExternalService.DpMeldekort
    )

    suspend fun fetchDigisosSakstema(user: UserPrincipal): ExternalResponse<Temaliste> = getDigisosSakstema(
        tokenSupplier = { tokenFetcher.digisosToken(user) },
        url = "$digisosUrl/minesaker/innsendte"
    )

    suspend fun fetchFoedselsdato(user: UserPrincipal): ExternalResponse<Foedselsdato> = withErrorHandling(
        tjeneste = ExternalService.Pdl,
        default = Foedselsdato(null, null)
    ) {
        pdlConsumer.hentFoedselsdato(user.ident, tokenFetcher.pdlToken(user))
    }

    private suspend fun getDigisosSakstema(
        tokenSupplier: suspend () -> String,
        url: String
    ): ExternalResponse<Temaliste> = withErrorHandling(
        tjeneste = ExternalService.Digisos,
        default = Temaliste(emptyList())
    ) {
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

                    Temaliste(temaer)
                }
                else -> throw HttpStatusException(response)
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
        tjeneste: ExternalService
    ): ExternalResponse<MeldekortStatus> = withErrorHandling(
        tjeneste = tjeneste,
        default = MeldekortStatus(false)
    ) {
        httpClient.get {
            url(url)
            header(HttpHeaders.Authorization, "Bearer ${tokenSupplier()}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header("Nav-Consumer-Id", "min-side:tms-mikrofrontend-selector")
        }.let { response ->

            when (response.status) {
                HttpStatusCode.OK -> {
                    val response: MeldekortDto = response.body()

                    MeldekortStatus(
                        harMeldekort = response.meldekortTilUtfylling.isNotEmpty()
                    )
                }
                HttpStatusCode.NotFound -> MeldekortStatus(
                    harMeldekort = false
                )
                else -> throw HttpStatusException(response)
            }
        }
    }

    private data class MeldekortDto(
        val meldekortTilUtfylling: List<Any> = emptyList()
    )

    private inline fun <reified T> withErrorHandling(
        tjeneste: ExternalService,
        default: T,
        function: () -> T
    ): ExternalResponse<T> {
        return try {
            ExternalResponse.ok(tjeneste, function())
        } catch (socketTimout: SocketTimeoutException) {
            ExternalResponse.error(
                default,
                tjeneste,
                "Fikk sockettimeout ved kall mot $tjeneste",
                socketTimout
            )
        } catch (requestTimeoutException: HttpRequestTimeoutException) {
            ExternalResponse.error(
                default,
                tjeneste,
                "Fikk requestTimeout ved kall mot $tjeneste",
                requestTimeoutException
            )
        } catch (tokenFetcherException: TokenFetcher.TokenFetcherException) {
            ExternalResponse.error(
                default,
                tjeneste,
                "Feil ved veksling av token for $tjeneste",
                tokenFetcherException
            )
        } catch (hse: HttpStatusException) {
            ExternalResponse.error(
                default,
                tjeneste,
                "Status fra ${hse.url} var ${hse.httpStatusCode}"
            )
        } catch (gqlError: GraphQlErrorException) {
            ExternalResponse.error(
                default,
                tjeneste,
                "Fikk ${gqlError.errors.size} feil i graphql-svar: ${gqlError.errors.joinToString()}"
            )
        } catch (gqlEmpty: GraphQlEmptyResponseException) {
            ExternalResponse.error(
                default,
                tjeneste,
                "Fikk tomt graphql-svar"
            )
        } catch (e: Exception) {
            throw ApiException(tjeneste, e)
        }
    }
}

class HttpStatusException(httpResponse: HttpResponse): Exception() {
    val httpStatusCode: HttpStatusCode = httpResponse.status
    val url: String = httpResponse.request.url.toString()
}

class GraphQlErrorException(val errors: List<String>): Exception()
class GraphQlEmptyResponseException(): Exception()

data class MeldekortStatus(
    val harMeldekort: Boolean
)

data class Tema(
    val kode: String,
    val sistEndret: LocalDateTime,
    val navn: String,
    val url: String
)

data class Temaliste(
    val temaer: List<Tema>
)

class ApiException(tjeneste: ExternalService, e: Exception) :
    Exception("Kall til ekstern tjeneste $tjeneste feiler.", e)
