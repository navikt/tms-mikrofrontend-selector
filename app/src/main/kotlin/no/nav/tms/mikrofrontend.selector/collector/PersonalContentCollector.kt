package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.*
import no.nav.tms.mikrofrontend.selector.collector.Produktkort.Companion.ids
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage

class PersonalContentCollector(
    val apiClient: HttpClient,
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage
) {

    fun getContent(ident: String, innloggetnivå: Int): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(ident)
        return PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions(innloggetnivå, manifestStorage.getManifestBucketContent())
                ?: emptyList(),
            produktkort = ProduktkortVerdier
                .resolveProduktkort(koder = apiClient.getProduktkort(ident), ident = ident, microfrontends = null)
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
        val id: String,
        val url: String
    )
}


