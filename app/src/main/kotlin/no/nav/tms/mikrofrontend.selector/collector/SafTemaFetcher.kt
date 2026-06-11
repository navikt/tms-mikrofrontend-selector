package no.nav.tms.mikrofrontend.selector.collector

import com.expediagroup.graphql.client.serialization.types.KotlinxGraphQLResponse
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import no.nav.dokument.saf.selvbetjening.generated.dto.HentTema
import java.time.Duration
import java.time.LocalDateTime

class SafTemaFetcher(
    private val httpClient: HttpClient,
    private val safUrl: String,
    private val dokumentarkivUrl: String
) {
    // Bruker async cache med await() for å beholde kompatibilitet med coroutines
    private val cache = Caffeine.newBuilder()
        .maximumSize(20000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .buildAsync<String, List<Tema>>()

    suspend fun hentTemaer(
        ident: String, token: String,
    ): List<Tema> = coroutineScope {
        val cacheGet = cache.get(ident) { _, _ ->
            future {
                fetchTemaer(ident, token)
            }
        }

        cacheGet.await()
    }

    private suspend fun fetchTemaer(
        ident: String, token: String,
    ): List<Tema> {
        val response = sendQuery(
            request = HentTema(HentTema.Variables(ident)),
            accessToken = token
        )

        if (!response.status.isSuccess()) {
            throw HttpStatusException(response)
        }

        val result = response.body<KotlinxGraphQLResponse<HentTema.Result>>()

        if (!result.errors.isNullOrEmpty()) {
            throw GraphQlErrorException(result.errors!!.map { it.message })
        } else if (result.data == null) {
            throw GraphQlEmptyResponseException()
        }

        return mapResponse(result.data!!)
    }

    private suspend fun sendQuery(request: HentTema, accessToken: String): HttpResponse =
        withContext(Dispatchers.IO) {
            httpClient.post {
                url("$safUrl/graphql")
                method = HttpMethod.Post
                header(Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
                timeout {
                    socketTimeoutMillis = 25000
                    connectTimeoutMillis = 10000
                    requestTimeoutMillis = 35000
                }
            }
        }

    private fun mapResponse(result: HentTema.Result): List<Tema> {
        return result.dokumentoversiktSelvbetjening
            .tema
            .map { tema ->
                val sistEndret = tema.journalposter
                    .filterNotNull()
                    .flatMap { it.relevanteDatoer }
                    .filterNotNull()
                    .maxOf { LocalDateTime.parse(it.dato) }

                Tema(
                    kode = tema.kode,
                    navn = tema.navn,
                    sistEndret = sistEndret,
                    url = dokumentarkivUrl
                )
            }
    }
}
