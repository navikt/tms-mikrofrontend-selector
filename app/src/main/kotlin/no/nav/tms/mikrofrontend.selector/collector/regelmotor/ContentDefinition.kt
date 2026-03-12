package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentRulesDefinition.Companion.initContentRules
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance

object ContentDefinition {
    private fun yamlObjectMapper() =
        ObjectMapper(YAMLFactory()).apply {
            registerModule(
                KotlinModule.Builder().build()
            )
        }

    private val contentYaml =
        object {}::class.java.getResource("/contentrules.yaml")?.readText().let { yaml ->
            yamlObjectMapper().readTree(yaml) ?: throw IllegalArgumentException("contentrules.yaml finnes ikke")
        }

    private val produktkort = contentYaml.initContentRules("produktkort", true)

    private val aktuelt = contentYaml.initContentRules("aktuelt", false)

    fun getAktueltContent(
        alder: Int,
        safDokument: List<Dokument>,
        discoveryManifest: DiscoveryManifest,
        levelOfAssurance: LevelOfAssurance
    ): List<MicrofrontendsDefinition> {
        val context = RuleContext(
            sakstemaKoder = safDokument.map { it.kode },
            dokumenter = safDokument,
            alder = alder,
            levelOfAssurance = levelOfAssurance
        )

        return aktuelt
            .map { RegelstyrtMicrofrontend.create(it, discoveryManifest) }
            .filter { it.skalVises(context) }
            .mapNotNull { it.definition }
    }

    fun getProduktkort(safDokument: List<Dokument>, levelOfAssurance: LevelOfAssurance): List<Produktkort> {
        val context = RuleContext(
            sakstemaKoder = safDokument.map { it.kode },
            dokumenter = safDokument,
            alder = null,
            levelOfAssurance = levelOfAssurance
        )

        return produktkort
            .map { Produktkort(id = it.id, rulesDefinition = it) }
            .filter { it.skalVises(context) }
    }
}