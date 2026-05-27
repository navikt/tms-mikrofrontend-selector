package no.nav.tms.mikrofrontend.selector.collector

import com.expediagroup.graphql.client.serialization.types.KotlinxGraphQLResponse
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.github.oshai.kotlinlogging.KotlinLogging
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dokument.saf.selvbetjening.generated.dto.HentTema
import no.nav.tms.common.logging.TeamLogs
import no.nav.tms.mikrofrontend.selector.DokumentarkivUrlResolver
import java.time.LocalDateTime

class SafConsumer(
    private val httpClient: HttpClient,
    private val safUrl: String,
    private val dokumentarkivUrlResolver: DokumentarkivUrlResolver
) {
    private val log = KotlinLogging.logger {}
    private val teamLog = TeamLogs.logger { }

    suspend fun hentTemaer(
        ident: String, token: String,
    ): SafResponse {
        val response = sendQuery(
            request = HentTema(HentTema.Variables(ident)),
            accessToken = token
        )

        // TODO mer utfyllende feil og logging
        if (!response.status.isSuccess()) {
            return ResponseWithErrors.createFromHttpError(response)
        }

        val result = response.body<KotlinxGraphQLResponse<HentTema.Result>>()

        if (!result.errors.isNullOrEmpty()) {
            return SafResponse(
                temaer = emptyList(),
                errors = result.errors?.map { it.message } ?: emptyList()
            )
        } else if (result.data == null) {
            return SafResponse(
                temaer = emptyList(),
                errors = emptyList()
            )
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

    private fun mapResponse(result: HentTema.Result): SafResponse {
        val temaer = result.dokumentoversiktSelvbetjening
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
                    url = dokumentarkivUrlResolver.urlFor(tema.kode)
                )
            }

        return SafResponse(temaer, emptyList())
    }
}

class SafResponse(
    val temaer: List<Tema>,
    errors: List<String>
) : ResponseWithErrors(errors.joinToString(";")) {
    override val source: String = "SAF"
}

data class Tema(
    val kode: String,
    val sistEndret: LocalDateTime,
    val navn: String,
    val url: String
)
