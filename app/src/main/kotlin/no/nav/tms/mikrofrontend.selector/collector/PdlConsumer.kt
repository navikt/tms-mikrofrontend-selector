package no.nav.tms.mikrofrontend.selector.collector

import com.expediagroup.graphql.client.serialization.types.KotlinxGraphQLResponse
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
import no.nav.pdl.generated.dto.HentFoedslsdato
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PdlConsumer(
    private val httpClient: HttpClient,
    private val pdlApiUrl: String,
    private val behandlingsNummer: String
) {
    suspend fun hentFoedselsdato(
        ident: String, token: String,
    ): Foedselsdato {
        val response = sendQuery(
            request = HentFoedslsdato(HentFoedslsdato.Variables(ident)),
            accessToken = token
        )

        // TODO mer utfyllende feil og logging
        if (!response.status.isSuccess()) {
            throw HttpStatusException(response)
        }

        val result = response.body<KotlinxGraphQLResponse<HentFoedslsdato.Result>>()

        if (!result.errors.isNullOrEmpty()) {
            throw GraphQlErrorException(result.errors!!.map { it.message })
        } else if (result.data == null) {
            throw GraphQlEmptyResponseException()
        }

        return mapResponse(result.data!!)
    }

    private suspend fun sendQuery(request: HentFoedslsdato, accessToken: String): HttpResponse =
        withContext(Dispatchers.IO) {
            httpClient.post {
                url("$pdlApiUrl/graphql")
                method = HttpMethod.Post
                header(Authorization, "Bearer $accessToken")
                header("Behandlingsnummer", behandlingsNummer)
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

    private fun mapResponse(result: HentFoedslsdato.Result): Foedselsdato {
        val foedselsdato = result.hentPerson
            ?.foedselsdato
            ?.mapNotNull { it.foedselsdato }
            ?.minOf(LocalDate::parse)

        val foedselsaar = result.hentPerson
            ?.foedselsdato
            ?.mapNotNull { it.foedselsaar }
            ?.min()

        return Foedselsdato(foedselsdato, foedselsaar)
    }
}

class Foedselsdato(
    val foedselsdato: LocalDate?,
    val foedselsaar: Int?
) {
    fun calculateAge() = when {
        foedselsdato != null -> ChronoUnit.YEARS.between(foedselsdato, LocalDate.now()).toInt()
        foedselsaar != null -> LocalDate.now().year - foedselsaar
        else -> 0
    }
}
