package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.collector.SafResponse

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

    val produktkortDefinitions = contentYaml["produktkort"].toList().map {
        val sakstemakoder = it.read<List<String>>("$.sakstemakoder")
            ?: throw IllegalArgumentException("Ett produktkort må ha minst 1 saks")
        ProduktkortDefinition(
            id = it.read<String>("$.id") ?: sakstemakoder.first(),
            navn = it.read<String>("$.navn")
                ?: throw IllegalArgumentException("navn må være definert for produktkort, $it"),
            sakstemakoder = it.read<List<String>>("$.sakstemakoder") ?: throw IllegalArgumentException(
                "Sakstemakoder må være definert for produktkort ${it["navn"].asText()}"
            ),
            ukerEtterSisteDokument = it.read<Int>("$.ukerEtterSisteDokument")
        )
    }

    val aktueltDefinitions =
        contentYaml["aktuelt"].toList().map {
            RegelstyrteMicrofrontendDefinitions(
                id = it["id"].asText(),
                exludeIfSakstema = it.read<List<String>>("$.excludeIfSakstema"),
                olderThanRule = it.read<Int>("$.eldreEnn")
            )
        }

    fun getAktueltContent(
        alder: Int,
        sakstemaer: List<String>,
        manifestMap: Map<String, String>
    ): List<MicrofrontendsDefinition> =
        aktueltDefinitions.map {
            RegelstyrtMicrofrontend(
                id = it.id,
                manifestMap = manifestMap,
            ).apply {
                if (it.olderThanRule != null)
                    contentRules.add(OlderThanContentRule(ageOfUser = alder, shouldBeOlderThan = it.olderThanRule))
                if (it.exludeIfSakstema != null) {
                    contentRules.add(
                        ExcludeIfSakstemaContentRule(
                            excludeList = it.exludeIfSakstema,
                            sakstemaer = sakstemaer
                        )
                    )
                }

            }
        }.filter { it.skalVises() }.mapNotNull { it.definition }

    fun getProduktkort(sakstemakoder: List<SafResponse.SafDokument>) = produktkortDefinitions.mapNotNull { definition ->
        sakstemakoder.find { definition.brukerHarSakstema(sakstemakoder) }?.let { safDokument ->
            Produktkort(id = definition.id, navn = definition.navn).apply {
                if (definition.ukerEtterSisteDokument != null)
                    rules.add(InPeriodContentRule(definition.ukerEtterSisteDokument, safDokument.sistEndret))
            }
        }
    }
}