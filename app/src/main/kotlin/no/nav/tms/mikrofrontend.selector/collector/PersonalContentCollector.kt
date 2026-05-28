package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import io.netty.channel.unix.Errors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PersonalContentCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val externalContentFetcher: ExternalContentFetcher,
    val produktkortCounter: ProduktkortCounter
) {

    suspend fun getContent(user: UserPrincipal): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return asyncCollector(user)
            .build(microfrontends, user.levelOfAssurance, manifestStorage.getDiscoveryManifest())
            .also {
                produktkortCounter.countProduktkort(it.produktkort)
            }
    }

    suspend fun asyncCollector(user: UserPrincipal) = coroutineScope {
        val safResponse = async { externalContentFetcher.fetchDocumentsFromSaf(user) }
        val meldekortApiResponse = async { externalContentFetcher.fetchFellesMeldekort(user) }
        val dpMeldekortResponse = async { externalContentFetcher.fetchDpMeldekort(user) }
        val pdlResponse = async { externalContentFetcher.fetchFoedselsdato(user) }
        val digisosResponse = async { externalContentFetcher.fetchDigisosSakstema(user) }

        PersonalContentFactory(
            safResponse = safResponse.await(),
            meldekortApiResponse = meldekortApiResponse.await(),
            dpMeldekortResponse = dpMeldekortResponse.await(),
            pdlResponse = pdlResponse.await(),
            digisosResponse = digisosResponse.await(),
            levelOfAssurance = user.levelOfAssurance
        )
    }
}

class PersonalContentFactory(
    val safResponse: ExternalResponse<Temaliste>,
    val meldekortApiResponse: ExternalResponse<MeldekortStatus>,
    val dpMeldekortResponse: ExternalResponse<MeldekortStatus>,
    val pdlResponse: ExternalResponse<Foedselsdato>,
    val digisosResponse: ExternalResponse<Temaliste>,
    val levelOfAssurance: LevelOfAssurance
) {

    fun build(
        microfrontends: Microfrontends?,
        levelOfAssurance: LevelOfAssurance,
        discoveryManifest: DiscoveryManifest,
    ): PersonalContentResponse {
        val errors = listOf(
            safResponse,
            meldekortApiResponse,
            dpMeldekortResponse,
            pdlResponse,
            digisosResponse
        )
            .filter { it.isError }
            .joinToString { it.getErrorMessage() }


        return PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions(levelOfAssurance, discoveryManifest) ?: emptyList(),
            produktkort = ContentDefinition.getProduktkort(
                digisosResponse.value.temaer + safResponse.value.temaer, levelOfAssurance
            ).filter { it.skalVises() }
                .map { it.id },
            offerStepup = microfrontends?.offerStepup(levelOfAssurance) ?: false,
            meldekort = meldekortApiResponse.value.harMeldekort || dpMeldekortResponse.value.harMeldekort,
            aktuelt = ContentDefinition.getAktueltContent(
                pdlResponse.value.calculateAge(),
                safResponse.value.temaer,
                discoveryManifest,
                levelOfAssurance
            ),
            errors = errors
        )
    }
}

class PersonalContentResponse(
    val microfrontends: List<MicrofrontendsDefinition>,
    val produktkort: List<String>,
    val offerStepup: Boolean,
    val meldekort: Boolean,
    val aktuelt: List<MicrofrontendsDefinition>,
    @JsonIgnore val errors: String?
) {
    fun resolveStatus(): HttpStatusCode =
        if (errors.isNullOrEmpty()) HttpStatusCode.OK else HttpStatusCode.MultiStatus
}

open class MicrofrontendsDefinition(
    @param:JsonProperty("microfrontend_id")
    val id: String,
    val url: String,
    val appname: String,
    val namespace: String,
    val fallback: String,
    val ssr: Boolean
) {
    companion object {
        fun create(id: String, discoveryManifest: DiscoveryManifest): MicrofrontendsDefinition? {
            val discovery = discoveryManifest.discovery[id];

            if (discovery != null) {
                return MicrofrontendsDefinition(
                    id,
                    discovery.url,
                    discovery.appname,
                    discovery.namespace,
                    discovery.fallback,
                    discovery.ssr
                )
            }

            return null
        }
    }
}
