package no.nav.tms.mikrofrontend.selector.collector

import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class TokenFetcher(
    private val tokendingsService: TokendingsService,
    private val meldekortApiClientId: String,
    private val dpMeldekortClientId: String,
    private val safClientId: String,
    private val pdlClientId: String,
    private val digisosClientId: String
) {
    suspend fun safToken(user: TokenXUser): String = fetchWithErrorHandling("SAF", safClientId, user)
    suspend fun meldekortApiToken(user: TokenXUser): String = fetchWithErrorHandling("meldekortApi", meldekortApiClientId, user)
    suspend fun dpMeldekortToken(user: TokenXUser): String = fetchWithErrorHandling("dpMeldekort", dpMeldekortClientId, user)
    suspend fun pdlToken(user: TokenXUser): String = fetchWithErrorHandling("pdl", pdlClientId, user)

    private suspend fun fetchWithErrorHandling(forService: String, appClientId: String, user: TokenXUser): String =
        try {
            tokendingsService.exchangeToken(token = user.tokenString, targetApp = appClientId)
        } catch (e: Exception) {
            throw TokenFetcherException(e, forService, appClientId)
        }

    suspend fun digisosToken(user: TokenXUser): String = fetchWithErrorHandling("digisos", digisosClientId, user)

    class TokenFetcherException(originalException: Exception, forService: String, appClientId: String) :
        Exception("henting av token for $forService med clientId $appClientId feiler: ${errorDetails(originalException)}")
}
