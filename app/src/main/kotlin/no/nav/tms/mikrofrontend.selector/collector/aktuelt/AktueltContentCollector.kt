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
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class AktueltCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val externalContentFecther: ExternalContentFecther,
) {

    suspend fun getAktuelt(user: TokenXUser, innloggetnivå: LevelOfAssurance): AktueltResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return asyncCollector(user).build(microfrontends, innloggetnivå, manifestStorage.getDiscoveryManifest())
    }

    suspend fun asyncCollector(user: TokenXUser) = coroutineScope {
        val safResponse = async { externalContentFecther.fetchDocumentsFromSaf(user) }
        val pdlResponse = async { externalContentFecther.fetchPersonOpplysninger(user) }

        return@coroutineScope AktueltFactory(
            safResponse = safResponse.await(),
            pdlResponse = pdlResponse.await(),
            levelOfAssurance = user.levelOfAssurance
        )

    }
}

class AktueltFactory(
    val safResponse: SafResponse,
    val pdlResponse: PdlResponse,
    val levelOfAssurance: LevelOfAssurance
) {

    fun build(
        microfrontends: Microfrontends?,
        levelOfAssurance: LevelOfAssurance,
        discoveryManifest: DiscoveryManifest,
    ) = AktueltResponse(
        offerStepup = microfrontends?.offerStepup(levelOfAssurance) ?: false,
        microfrontends = ContentDefinition.getAktueltContent(
            pdlResponse.calculateAge(),
            safResponse.dokumenter,
            discoveryManifest,
            levelOfAssurance
        )

    ).apply {
        errors = listOf(
            safResponse,
            pdlResponse,
        ).mapNotNull { it.errorMessage() }.joinToString()
    }
}

class AktueltResponse(
    val offerStepup: Boolean,
    val microfrontends: List<MicrofrontendsDefinition>
) {
    @JsonIgnore
    var errors: String? = null

    fun resolveStatus(): HttpStatusCode =
        if (errors.isNullOrEmpty()) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
}
