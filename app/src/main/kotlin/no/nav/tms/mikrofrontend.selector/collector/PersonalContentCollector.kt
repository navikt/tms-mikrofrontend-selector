package no.nav.tms.mikrofrontend.selector.collector

import io.ktor.client.*
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

class PersonalContentCollector(
    val apiClient: HttpClient,
    val repository: PersonRepository
) {

    fun getContent(ident: String): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(ident)
        val produktkort = apiClient.getProduktkort(ident)
        return PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions() ?: emptyList(),
            produktkort = produktkort
                .map { safkode -> produktkortene[safkode] }
                .filterNotNull()
                .filter { it.skalVises() }
                .map { it.id },
            offerStepup = microfrontends?.offerStepup() ?: false
        )
    }

    private fun HttpClient.getProduktkort(ident: String): List<String> {
        TODO("Not yet implemented")
    }
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