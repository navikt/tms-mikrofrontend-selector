package no.nav.tms.mikrofrontend.selector.collector

import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class TokenFetcher(
    private val tokendingsService: TokendingsService,
    private val meldekortClientId: String,
    private val oppfølgingClientId: String,
    private val safClientId: String,
    private val pdlClientId: String,
    private val legacyDigisosClientId: String,
    private val digisosClientId: String,

    ) {
    suspend fun safToken(user: TokenXUser): String = fetchWithErrorHandling("SAF", safClientId, user)

    suspend fun oppfolgingToken(user: TokenXUser): String =
        fetchWithErrorHandling("Oppfølging", oppfølgingClientId, user)

    suspend fun meldekortToken(user: TokenXUser): String = fetchWithErrorHandling("meldekort", meldekortClientId, user)

    suspend fun pdlToken(user: TokenXUser): String = fetchWithErrorHandling("pdl", pdlClientId, user)

    private suspend fun fetchWithErrorHandling(forService: String, appClientId: String, user: TokenXUser): String =
        try {
            tokendingsService.exchangeToken(token = user.tokenString, targetApp = appClientId)
        } catch (e: Exception) {
            throw TokenFetcherException(e, forService, appClientId)
        }

    suspend fun legacyDigisosToken(user: TokenXUser): String = fetchWithErrorHandling("digisos", legacyDigisosClientId, user)
    suspend fun digisosToken(user: TokenXUser): String = fetchWithErrorHandling("digisos", digisosClientId, user)

    class TokenFetcherException(originalException: Exception, forService: String, appClientId: String) :
        Exception("henting av token for $forService med clientId $appClientId feiler: ${errorDetails(originalException)}")
}