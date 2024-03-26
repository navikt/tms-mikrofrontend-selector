package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import no.nav.tms.mikrofrontend.selector.database.Microfrontends
import java.time.LocalDateTime

abstract class ContentRule {
    abstract fun applyRule(): Boolean
}

class Produktkort(
    val id: String,
    val navn: String,
    val rules: List<ContentRule> = emptyList()
) {

    fun skalVises() =
        rules.all { it.applyRule() }
}

class ProduktkortDefinition(
    val id: String,
    val navn: String,
    val sakstemakoder: List<String>,
    val ukerEtterSisteDokument: Int?
) {
    fun brukerHarSakstema(
        safDokumenter: List<SafDokument>
    ): Boolean =
        sakstemakoder.intersect(safDokumenter.map { it.sakstemakode }.toSet()).isNotEmpty()

}
object Produktfactory {
    private val yamlObjectMapper =
        ObjectMapper(YAMLFactory()).apply {
            registerModule(
                KotlinModule.Builder().build()
            )
        }
    val produktkortYaml =
        object{}::class.java.getResource("/contentrules.yaml")?.readText().let { yaml ->
        yamlObjectMapper.readTree(yaml) ?: throw IllegalArgumentException("contentrules.yaml finnes ikke")
    }

    private val produktkortDefinitions = produktkortYaml["produktkort"].toList().map {
        val sakstemakoder = it.read<List<String>>("$.sakstemakoder")
            ?: throw IllegalArgumentException("Ett produktkort må ha mins 1 saks")
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

    fun getProduktkort(sakstemakoder: List<SafDokument>) = produktkortDefinitions.mapNotNull { definition ->
        sakstemakoder.find { definition.brukerHarSakstema(sakstemakoder) }?.let { safDokument ->
            val contentRules = mutableListOf<ContentRule>().apply {
                if (definition.ukerEtterSisteDokument != null)
                    add(IsInPeriodContentRule(definition.ukerEtterSisteDokument, safDokument.sistEndret))
            }
            Produktkort(id = definition.id, navn = definition.navn, rules = contentRules)
        }
    }

    class IsInPeriodContentRule(val ukerEtterSisteDokument: Int, val dokumentSistEndret: LocalDateTime) :
        ContentRule() {
        override fun applyRule(): Boolean =
            dokumentSistEndret.isAfter(LocalDateTime.now().minusWeeks(ukerEtterSisteDokument.toLong()))
    }

}


/*
enum class ProduktkortVerdier(val produktkort: Produktkort) {
    DAG(Produktkort(id = "DAG", navn = "Dagpenger")),
    FOR(Produktkort(id = "FOR", navn = "Foreldrepenger")),
    HJE(Produktkort(id = "HJE", navn = "Hjelpemidler")),
    KOM(Produktkort(id = "KOM", navn = "Sosialhjelp")),
    PEN(Produktkort(id = "PEN", navn = "Pensjon")),
    UFO(Produktkort(id = "UFO", navn = "Uføretrygd")),
    SYK(Produktkort(id = "SYK", navn = "Sykefravær")),
    SYM(Produktkort(id = "SYK", navn = "Sykefravær"));

    companion object {
        private val values = ProduktkortVerdier.entries.map { it.name }
        fun resolveProduktkort(koder: List<String>, microfrontends: Microfrontends?): List<Produktkort> =
            koder.mapNotNull { kode ->
                if (values.contains(kode)) {
                    ProduktkortVerdier.valueOf(kode).produktkort
                        .let {
                            if (it.skalVises()) {
                                it
                            } else null
                        }
                } else null
            }.distinctBy { it.id }
    }
}
*/

