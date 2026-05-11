package no.nav.tms.mikrofrontend.selector.collector

import no.nav.tms.token.support.user.token.exchange.UserTokenExchanger
import no.nav.tms.token.support.user.token.verification.UserPrincipal

class TokenFetcher(
    private val tokendingsService: UserTokenExchanger,
    private val meldekortApiClientId: String,
    private val dpMeldekortClientId: String,
    private val safClientId: String,
    private val pdlClientId: String,
    private val digisosClientId: String
) {
    suspend fun safToken(user: UserPrincipal): String = fetchWithErrorHandling("SAF", safClientId, user)
    suspend fun meldekortApiToken(user: UserPrincipal): String = fetchWithErrorHandling("meldekortApi", meldekortApiClientId, user)
    suspend fun dpMeldekortToken(user: UserPrincipal): String = fetchWithErrorHandling("dpMeldekort", dpMeldekortClientId, user)
    suspend fun pdlToken(user: UserPrincipal): String = fetchWithErrorHandling("pdl", pdlClientId, user)

    private suspend fun fetchWithErrorHandling(forService: String, appClientId: String, user: UserPrincipal): String =
        try {
            tokendingsService.exchangeToken(token = user.accessToken, targetApp = appClientId)
        } catch (e: Exception) {
            throw TokenFetcherException(e, forService, appClientId)
        }

    suspend fun digisosToken(user: UserPrincipal): String = fetchWithErrorHandling("digisos", digisosClientId, user)

    class TokenFetcherException(originalException: Exception, forService: String, appClientId: String) :
        Exception("henting av token for $forService med clientId $appClientId feiler: ${errorDetails(originalException)}")
}
