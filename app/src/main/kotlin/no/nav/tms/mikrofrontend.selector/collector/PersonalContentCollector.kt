package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class PersonalContentCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val externalContentFecther: ExternalContentFecther,
    val produktkortCounter: ProduktkortCounter
) {

    suspend fun getContent(user: TokenXUser, innloggetnivå: LevelOfAssurance): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return asyncCollector(user).build(microfrontends, innloggetnivå, manifestStorage.getManifestBucketContent())
            .also {
                produktkortCounter.countProduktkort(it.produktkort)
            }
    }

    suspend fun asyncCollector(user: TokenXUser) = coroutineScope {
        val safResponse = async { externalContentFecther.fetchDocumentsFromSaf(user) }
        val meldekortResponse = async { externalContentFecther.fetchMeldekort(user) }
        val pdlResponse = async { externalContentFecther.fetchPersonOpplysninger(user) }
        val digisosResponse = async { externalContentFecther.fetchDigisosSakstema(user) }

        return@coroutineScope PersonalContentFactory(
            safResponse = safResponse.await(),
            meldekortResponse = meldekortResponse.await(),
            pdlResponse = pdlResponse.await(),
            digisosResponse = digisosResponse.await(),
            levelOfAssurance = user.levelOfAssurance
        )

    }
}

class PersonalContentFactory(
    val safResponse: SafResponse,
    val meldekortResponse: MeldekortResponse,
    val pdlResponse: PdlResponse,
    val digisosResponse: DigisosResponse,
    val levelOfAssurance: LevelOfAssurance
) {

    fun build(
        microfrontends: Microfrontends?,
        levelOfAssurance: LevelOfAssurance,
        manifestMap: Map<String, String>,
    ) = PersonalContentResponse(
        microfrontends = microfrontends?.getDefinitions(levelOfAssurance, manifestMap) ?: emptyList(),
        produktkort = ContentDefinition.getProduktkort(
            digisosResponse.dokumenter + safResponse.dokumenter, levelOfAssurance
        ).filter { it.skalVises() }.map { it.id },
        offerStepup = microfrontends?.offerStepup(levelOfAssurance) ?: false,
        meldekort = meldekortResponse.harMeldekort,
        aktuelt = ContentDefinition.getAktueltContent(
            pdlResponse.calculateAge(),
            safResponse.dokumenter,
            manifestMap,
            levelOfAssurance
        )

    ).apply {
        errors = listOf(
            safResponse,
            meldekortResponse,
            pdlResponse,
            digisosResponse
        ).mapNotNull { it.errorMessage() }.joinToString()
    }
}

class PersonalContentResponse(
    val microfrontends: List<MicrofrontendsDefinition>,
    val produktkort: List<String>,
    val offerStepup: Boolean,
    val meldekort: Boolean,
    val aktuelt: List<MicrofrontendsDefinition>
) {
    @JsonIgnore
    var errors: String? = null
    fun resolveStatus(): HttpStatusCode =
        if (errors.isNullOrEmpty()) HttpStatusCode.OK else HttpStatusCode.MultiStatus
}

open class MicrofrontendsDefinition(
    @JsonProperty("microfrontend_id")
    val id: String,
    val url: String
) {
    companion object {
        fun create(id: String, manifestMap: Map<String, String>) = manifestMap[id]
            .takeUnless {
                it.isNullOrEmpty()
            }
            ?.let { url ->
                MicrofrontendsDefinition(id, url)
            }
    }
}
