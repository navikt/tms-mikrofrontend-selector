package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import no.nav.tms.mikrofrontend.selector.collector.regelmotor.ContentRulesFactory.Companion.initContentRuleFactory

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

    private val produktkortFactory = contentYaml.initContentRuleFactory("produktkort", true)

    private val aktueltFactory = contentYaml.initContentRuleFactory("aktuelt", false)

    fun getAktueltContent(
        alder: Int,
        safDokument: List<SafDokument>,
        manifestMap: Map<String, String>
    ): List<MicrofrontendsDefinition> =
        aktueltFactory.map {
            RegelstyrtMicrofrontend(id = it.id, manifestMap = manifestMap).apply {
                contentResolvers = it.createRules(safDokument, alder)
            }
        }.filter { it.skalVises() }.mapNotNull { it.definition }

    fun getProduktkort(safDokument: List<SafDokument>) = produktkortFactory.map { definition ->
        Produktkort(id = definition.id).apply {
            rules = definition.createRules(safDokumenter = safDokument, alder = null)
        }
    }.filter { it.skalVises() }
}