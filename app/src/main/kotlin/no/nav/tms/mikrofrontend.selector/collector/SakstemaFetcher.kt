package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.*
import io.ktor.client.request.*
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class SakstemaFetcher(
    val safUrl: String,
    val safClientId: String,
    val httpClient: HttpClient,
    val tokendingsService: TokendingsService
) {

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
        url(safUrl)
        header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, safClientId)}")
        header("Content-Type", "application/json")
        setBody(query(user.ident))
    }
}