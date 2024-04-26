package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentDefinition
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.Section
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class PersonalContentCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val externalContentFecther: ExternalContentFecther,
    val produktkortCounter: ProduktkortCounter
) {

    suspend fun getContent(user: TokenXUser, innloggetnivå: Int): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        return asyncCollector(user).build(microfrontends, innloggetnivå, manifestStorage.getManifestBucketContent())
            .also {
                produktkortCounter.countProduktkort(it.produktkort)
            }
    }


    suspend fun asyncCollector(user: TokenXUser): PersonalContentFactory {
        return coroutineScope {
            val safResponse = async { externalContentFecther.fetchSakstema(user) }
            val arbeidsøkerResponse = async { externalContentFecther.fetchArbeidsøker(user) }
            val oppfolgingResponse = async { externalContentFecther.fetchOppfolging(user) }
            val meldekortResponse = async { externalContentFecther.fetchMeldekort(user) }
            val pdlResponse = async { externalContentFecther.fetchPersonOpplysninger(user) }
            val digisosResponse = async { externalContentFecther.fetchDigisosSakstema(user) }

            return@coroutineScope PersonalContentFactory(
                arbeidsøkerResponse = arbeidsøkerResponse.await(),
                safResponse = safResponse.await(),
                meldekortResponse = meldekortResponse.await(),
                oppfolgingResponse = oppfolgingResponse.await(),
                pdlResponse = pdlResponse.await(),
                digisosResponse = digisosResponse.await()
            )
        }
    }
}

class PersonalContentFactory(
    val arbeidsøkerResponse: ArbeidsøkerResponse,
    val safResponse: SafResponse,
    val meldekortResponse: MeldekortResponse,
    val oppfolgingResponse: OppfolgingResponse,
    val pdlResponse: PdlResponse,
    val digisosResponse: DigisosResponse
) {
    fun build(
        microfrontends: Microfrontends?,
        innloggetnivå: Int,
        manifestMap: Map<String, String>,
    ): PersonalContentResponse {
        val arbeidsøkerSection = ContentDefinition.arbeidsøkerSection.getMicrofrontendsForSection(microfrontends)

        return PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions(innloggetnivå, manifestMap) ?: emptyList(),
            produktkort = ContentDefinition.getProduktkort(
                digisosResponse.dokumenter + safResponse.dokumenter
            ).filter { it.skalVises() }.map { it.id },

            offerStepup = microfrontends?.offerStepup(innloggetnivå) ?: false,
            aiaStandard = arbeidsøkerResponse.isStandardInnsats() && arbeidsøkerSection.isEmpty(),
            // || arbeidsøkerResponse.brukNyAia?:false skal fjernes når ny-aia er over på kafka
            brukNyAia = arbeidsøkerSection.isNotEmpty() || arbeidsøkerResponse.brukNyAia?:false,
            oppfolgingContent = oppfolgingResponse.underOppfolging,
            meldekort = meldekortResponse.harMeldekort,
            aktuelt = ContentDefinition.getAktueltContent(
                pdlResponse.calculateAge(),
                safResponse.dokumenter,
                manifestMap
            )

        ).apply {
            errors = listOf(
                arbeidsøkerResponse,
                safResponse,
                meldekortResponse,
                oppfolgingResponse,
                pdlResponse
            ).mapNotNull { it.errorMessage() }.joinToString()
        }
    }
}

class PersonalContentResponse(
    val microfrontends: List<MicrofrontendsDefinition>,
    val produktkort: List<String>,
    val offerStepup: Boolean,
    val aiaStandard: Boolean,
    val brukNyAia: Boolean,
    val oppfolgingContent: Boolean,
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