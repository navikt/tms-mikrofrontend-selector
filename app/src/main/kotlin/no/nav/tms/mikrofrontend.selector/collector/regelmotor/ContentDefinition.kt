package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentRulesDefinition.Companion.parseContentRuleDefinitions

object ContentDefinition {
    private val yamlObjectMapper =
        ObjectMapper(YAMLFactory()).apply {
            registerModule(
                KotlinModule.Builder().build()
            )
        }
    private val contentYaml =
        object {}::class.java.getResource("/contentrules.yaml")?.readText().let { yaml ->
            yamlObjectMapper.readTree(yaml) ?: throw IllegalArgumentException("contentrules.yaml finnes ikke")
        }

    private val produktkortDefinitions = contentYaml.parseContentRuleDefinitions("produktkort", true)

    val aktueltDefinitions =
        contentYaml.parseContentRuleDefinitions("aktuelt", false)

    fun getAktueltContent(
        alder: Int,
        safDokument: List<SafDokument>,
        manifestMap: Map<String, String>
    ): List<MicrofrontendsDefinition> =
        aktueltDefinitions.map {
            RegelstyrtMicrofrontend(
                id = it.id,
                manifestMap = manifestMap,
            ).apply {
                contentRules = it.createRules(safDokument, alder)
            }
        }.filter { it.skalVises() }.mapNotNull { it.definition }

    fun getProduktkort(safDokument: List<SafDokument>) = produktkortDefinitions.map { definition ->
        Produktkort(id = definition.id).apply {
            rules = definition.createRules(safDokumenter = safDokument, alder = null)
        }
    }.filter { it.skalVises() }
}