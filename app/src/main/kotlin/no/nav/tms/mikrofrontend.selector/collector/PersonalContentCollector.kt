package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tms.mikrofrontend.selector.collector.Produktkort.Companion.ids
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class PersonalContentCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val servicesFetcher: ServicesFetcher,
    val produktkortCounter: ProduktkortCounter
) {
    suspend fun getContent(user: TokenXUser, innloggetnivå: Int): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return asyncCollector(user).build(microfrontends,innloggetnivå,manifestStorage.getManifestBucketContent()).also {
            produktkortCounter.countProduktkort(it.produktkort)
        }
    }


    suspend fun asyncCollector(user: TokenXUser): PersonalContentFactory {
        return coroutineScope {
            val safResponse = async { servicesFetcher.fetchSakstema(user) }
            val arbeidsøkerResponse = async { servicesFetcher.fetchArbeidsøker(user) }
            val oppfolgingResponse = async { servicesFetcher.fetchOppfolging(user) }
            val meldekortResponse = async { servicesFetcher.fetchMeldekort(user) }
            return@coroutineScope PersonalContentFactory(
                arbeidsøkerResponse = arbeidsøkerResponse.await(),
                safResponse = safResponse.await(),
                meldekortResponse = meldekortResponse.await(),
                oppfolgingResponse = oppfolgingResponse.await()
            )
        }
    }
}

class PersonalContentFactory(
    val arbeidsøkerResponse: ArbeidsøkerResponse,
    val safResponse: SafResponse,
    val meldekortResponse: MeldekortResponse,
    val oppfolgingResponse: OppfolgingResponse
) {
    fun build(
        microfrontends: Microfrontends?,
        innloggetnivå: Int,
        manifestMap: Map<String, String>,
    ): PersonalContentResponse =
        PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions(innloggetnivå, manifestMap)?: emptyList(),
            produktkort = ProduktkortVerdier
                .resolveProduktkort(
                    koder = safResponse.sakstemakoder,
                    microfrontends = microfrontends
                ).ids(),
            offerStepup = microfrontends?.offerStepup(innloggetnivå)?:false,
            aiaStandard = arbeidsøkerResponse.erStandard && arbeidsøkerResponse.erArbeidssoker,
            oppfolgingContent = oppfolgingResponse.underOppfolging,
            meldekort = meldekortResponse.harMeldekort
        ).apply {
            errors = listOf(
                arbeidsøkerResponse,
                safResponse,
                meldekortResponse,
                oppfolgingResponse
            ).mapNotNull { it.errorMessage() }.joinToString()
        }
}

class PersonalContentResponse(
    val microfrontends: List<MicrofrontendsDefinition>,
    val produktkort: List<String>,
    val offerStepup: Boolean,
    val aiaStandard: Boolean,
    val oppfolgingContent: Boolean,
    val meldekort: Boolean
) {
    @JsonIgnore
    var errors: String? = null
    fun resolveStatus(): HttpStatusCode =
        if (errors.isNullOrEmpty()) HttpStatusCode.OK else HttpStatusCode.MultiStatus
}

data class MicrofrontendsDefinition(
    @JsonProperty("microfrontend_id")
    val id: String,
    val url: String
)
