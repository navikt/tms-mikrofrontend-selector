package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class SakstemaFetcher(
    val safUrl: String,
    val safClientId: String,
    val httpClient: HttpClient,
    val tokendingsService: TokendingsService
) {

    val log = KotlinLogging.logger { }
    val objectMapper = jacksonObjectMapper()

    fun query(ident: String) = """
        query {
            dokumentoversiktSelvbetjening(ident: \"$ident\", tema: []) {
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
    """.trimIndent()

    suspend fun fetchSakstema(user: TokenXUser) = httpClient.post {
        url("$safUrl/graphql")
        header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, safClientId)}")
        header("Content-Type", "application/json")
        setBody(query(user.ident))
    }
        .let {
            if (it.status != HttpStatusCode.OK) throw SafRequestException("Kall til SAF feilet", statusCode = it.status)
            objectMapper.readTree(it.bodyAsText())["data"]["dokumentoversiktSelvbetjening"]["tema"]
                .toList()
                .map { node -> node["kode"].asText() }
        }
}

class SafRequestException(message: String, val statusCode: HttpStatusCode): Exception(message)