package no.nav.tms.mikrofrontend.selector.collector

import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class TokenFetcher(
    private val tokendingsService: TokendingsService,
    private val meldekortClientId: String,
    private val oppfølgingClientId: String,
    private val safClientId: String,
    private val aiaClientId: String

) {
    suspend fun safToken(user: TokenXUser): String = fetchWithErrorHandling("SAF", safClientId, user)

    suspend fun oppfolgingToken(user: TokenXUser): String =
        fetchWithErrorHandling("Oppfølging", oppfølgingClientId, user)

    suspend fun aiaToken(user: TokenXUser): String = fetchWithErrorHandling("aia-backend", aiaClientId, user)

    suspend fun meldekortToken(user: TokenXUser): String = fetchWithErrorHandling("meldekort", meldekortClientId, user)

    private suspend fun fetchWithErrorHandling(forService: String, appClientId: String, user: TokenXUser): String =
        try {
            tokendingsService.exchangeToken(token = user.tokenString, targetApp = appClientId)
        } catch (e: Exception) {
            throw TokenFetcherException(e, forService, appClientId)
        }

    class TokenFetcherException(originalException: Exception, forService: String, appClientId: String) :
        Exception("henting av token for $forService med clientId $appClientId feiler: ${errorDetails(originalException)}")
}