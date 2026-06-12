package no.nav.tms.mikrofrontend.selector.collector.aktuelt

import no.nav.tms.mikrofrontend.selector.collector.*
import com.fasterxml.jackson.annotation.JsonIgnore
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.UserPrincipal

class AktueltCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val externalContentFetcher: ExternalContentFetcher,
) {

    suspend fun getAktuelt(user: UserPrincipal): AktueltResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return asyncCollector(user).build(microfrontends, user.levelOfAssurance, manifestStorage.getDiscoveryManifest())
    }

    suspend fun asyncCollector(user: UserPrincipal) = coroutineScope {
        val safResponse = async { externalContentFetcher.fetchDocumentsFromSaf(user) }
        val pdlResponse = async { externalContentFetcher.fetchFoedselsdato(user) }

        AktueltFactory(
            safResponse = safResponse.await(),
            pdlResponse = pdlResponse.await(),
            levelOfAssurance = user.levelOfAssurance
        )

    }
}

class AktueltFactory(
    val safResponse: ExternalResponse<List<Tema>>,
    val pdlResponse: ExternalResponse<Foedselsdato>,
    val levelOfAssurance: LevelOfAssurance
) {

    fun build(
        microfrontends: Microfrontends?,
        levelOfAssurance: LevelOfAssurance,
        discoveryManifest: DiscoveryManifest,
    ) = AktueltResponse(
        offerStepup = microfrontends?.offerStepup(levelOfAssurance) ?: false,
        microfrontends = ContentDefinition.getAktueltContent(
            pdlResponse.value.calculateAge(),
            safResponse.value,
            discoveryManifest,
            levelOfAssurance
        ),
        errors = listOf(
            safResponse,
            pdlResponse,
        )
            .filter { it.isError }
            .joinToString()
    )
}

class AktueltResponse(
    val offerStepup: Boolean,
    val microfrontends: List<MicrofrontendsDefinition>,
    @JsonIgnore
    var errors: String?
) {

    fun resolveStatus(): HttpStatusCode =
        if (errors.isNullOrEmpty()) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
}
