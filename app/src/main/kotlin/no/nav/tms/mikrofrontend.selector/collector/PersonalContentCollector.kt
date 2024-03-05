package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import no.nav.tms.mikrofrontend.selector.collector.Produktkort.Companion.ids
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class PersonalContentCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val sakstemaFetcher: SakstemaFetcher
) {

    suspend fun getContent(user: TokenXUser, innloggetnivå: Int): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions(innloggetnivå, manifestStorage.getManifestBucketContent())
                ?: emptyList(),
            produktkort = ProduktkortVerdier
                .resolveProduktkort(koder = sakstemaFetcher.fetchSakstema(user), ident = user.ident, microfrontends = null)
                .ids(),
            offerStepup = microfrontends?.offerStepup(innloggetnivå = innloggetnivå) ?: false

        )
    }

    private fun HttpClient.getProduktkort(ident: String): List<String> {
        TODO("Not yet implemented")
    }

    class PersonalContentResponse(
        val microfrontends: List<MicrofrontendsDefinition>,
        val produktkort: List<String>,
        val offerStepup: Boolean
    )

    class MicrofrontendsDefinition(
        @JsonProperty("microfrontend_id")
        val id: String,
        val url: String
    )
}


