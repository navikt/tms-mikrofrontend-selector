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

    fun query(ident: String) = """ {
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

    private fun String.compactJson(): String =
        trimIndent()
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")


    suspend fun fetchSakstema(user: TokenXUser): SafResponse {
        val token = tokendingsService.exchangeToken(user.tokenString, safClientId)
        log.info { query(user.ident) }

        return httpClient.post {
            url("$safUrl/graphql")
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            setBody(query(user.ident))
        }
            .let { response ->
                log.info { "Mottok svar fra SAF" }
                val body = response.bodyAsText().also { log.info { "body: $it" } }
                val safResponse = objectMapper.readTree(body)
                when {
                    response.status != HttpStatusCode.OK -> SafResponse(emptyList(), listOf("Kall til SAF feilet med statuskode ${response.status}"))

                    safResponse["errors"] != null && !safResponse["errors"].isMissingOrNull() -> SafResponse(
                        emptyList(),
                        safResponse["errors"].toList().map { it["message"].asText() })

                    else -> safResponse["data"]["dokumentoversiktSelvbetjening"]["tema"]
                        .toList()
                        .map { node -> node["kode"].asText() }
                        .let {
                            SafResponse(it, emptyList())
                        }
                }

            }

    }
}

class SafResponse(val sakstemakoder: List<String>, val errors: List<String>) {
    val hasErrors = errors.isNotEmpty()
}