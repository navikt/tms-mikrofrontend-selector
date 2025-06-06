package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentRulesDefinition.Companion.initContentRules
import no.nav.tms.mikrofrontend.selector.versions.MicrofrontendManifest
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
        manifestMap: MicrofrontendManifest,
        levelOfAssurance: LevelOfAssurance
    ): List<MicrofrontendsDefinition> =
        aktuelt.map {
            RegelstyrtMicrofrontend(id = it.id, manifestMap = manifestMap).apply {
                contentResolvers = it.createRules(safDokument, alder, levelOfAssurance)
            }
        }.filter { it.skalVises() }.mapNotNull { it.definition }

    fun getProduktkort(safDokument: List<Dokument>, levelOfAssurance: LevelOfAssurance) = produktkort.map { definition ->
        Produktkort(id = definition.id).apply {
            rules = definition.createRules(safDokumenter = safDokument, alder = null, userLevelOfAssurance = levelOfAssurance)
        }
    }.filter { it.skalVises() }
}