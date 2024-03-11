package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.rapids_rivers.isMissingOrNull
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

    suspend fun fetchSakstema(user: TokenXUser) {
        log.info { "Heneter sakstema fra $safUrl/graphql" }
        httpClient.post {
            url("$safUrl/graphql")
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, safClientId)}")
            header("Content-Type", "application/json")
            try {
                setBody(query(user.ident))
            } catch (e: Exception) {
                throw SafRequestException(
                    "Post kall feilet pga. feil i JSON body ${query(user.ident)}",
                    statusCode = HttpStatusCode.InternalServerError
                )
            }
        }.let {
            log.info { "Mottok svar fra SAF" }
            val body = it.bodyAsText().also { log.info { "body: $it" } }
            val safResponse = objectMapper.readTree(body)
            if (!safResponse["errors"].isMissingOrNull()) throw SafRequestException(
                "Kall til SAF feilet",
                statusCode = HttpStatusCode.MultiStatus
            )
            log.info { "body: $body" }
            try {
                safResponse["data"]["dokumentoversiktSelvbetjening"]["tema"]
                    .toList()
                    .map { node -> node["kode"].asText() }
            } catch (e: Exception) {
                log.info { body }
                throw SafRequestException(
                    "Kall til SAF feilet: ${e.javaClass.name}",
                    statusCode = HttpStatusCode.InternalServerError
                )
            }

        }
    }
}

class SafRequestException(message: String, val statusCode: HttpStatusCode) : Exception(message)