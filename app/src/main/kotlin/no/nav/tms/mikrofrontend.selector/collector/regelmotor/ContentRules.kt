package no.nav.tms.mikrofrontend.selector.collector.regelmotor


import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import java.time.LocalDateTime

interface ContentRule {
    fun skalVises(): Boolean
}

class ContentRulesDefinition(
    val id: String,
    val includeIfSakstema: List<String>?,
    val exludeIfSakstema: List<String>?,
    val olderThanRule: Int?,
    val ukerEtterSisteDokument: UkeEtterSisteDokument?
) {

    fun createRules(safDokumenter: List<SafDokument>, alder: Int?) = mutableListOf<ContentRule>().apply {
        addIfNotNull(includeIfSakstema) {
            IncludeIfSakstemaContentRule(
                includeList = it,
                sakstemaer = safDokumenter.map { dok -> dok.sakstemakode })
        }
        addIfNotNull(exludeIfSakstema) {
            ExcludeIfSakstemaContentRule(
                excludeList = it,
                sakstemaer = safDokumenter.map { dok -> dok.sakstemakode })
        }
        addIfNotNull(olderThanRule, alder) { olderThan, age ->
            OlderThanContentRule(shouldBeOlderThan = olderThan, ageOfUser = age)
        }
        addIfNotNull(ukerEtterSisteDokument) {
            LastDocumentAfterContentRule(
                sakstemakode = it.sakstemakode,
                periodInWeeks = it.antallUker,
                safDokumenter = safDokumenter
            )
        }
    }

    private fun <T> MutableList<ContentRule>.addIfNotNull(value: T?, construct: (T) -> ContentRule) {
        if (value != null) {
            add(construct(value))
        }
    }

    private fun <T, R> MutableList<ContentRule>.addIfNotNull(value: T?, val2: R?, construct: (T, R) -> ContentRule) {
        if (value != null && val2 != null) {
            add(construct(value, val2))
        }
    }

    companion object {
        fun JsonNode.parseContentRuleDefinitions(category: String, requireSakstemakode: Boolean) =
            this[category].toList().map {
                ContentRulesDefinition(
                    id = it.read<String>("$.id") ?: throw IllegalArgumentException("contentrules må ha en id"),
                    includeIfSakstema = it.read<List<String>>("$.includeIfSakstema").let { sakstema ->
                        if (requireSakstemakode && sakstema == null) {
                            throw IllegalArgumentException("sakstemakoder er påkrevd for innhold av type $category")
                        } else sakstema
                    },
                    ukerEtterSisteDokument = it.read<JsonNode>("$.ukerEtterSisteDokument")?.let {
                        UkeEtterSisteDokument(
                            it.read<Int>("$.antallUker")
                                ?: throw IllegalArgumentException("Antall uker må være definert for ukerEtterSisteDokumentRegler"),
                            it.read<String>("$.kode")
                                ?: throw IllegalArgumentException("kode må være definert for ukerEtterSisteDokumentRegler"),
                        )
                    },
                    exludeIfSakstema = it.read<List<String>>("$.excludeIfSakstema"),
                    olderThanRule = it.read<Int>("$.eldreEnn")
                )
            }
    }

    class UkeEtterSisteDokument(val antallUker: Int, val sakstemakode: String)
}

class OlderThanContentRule(val shouldBeOlderThan: Int, val ageOfUser: Int) : ContentRule {
    override fun skalVises(): Boolean = ageOfUser > shouldBeOlderThan
}

class ExcludeIfSakstemaContentRule(val excludeList: List<String>, val sakstemaer: List<String>) : ContentRule {
    override fun skalVises(): Boolean = excludeList.intersect(sakstemaer.toSet()).isEmpty()
}

open class IncludeIfSakstemaContentRule(val includeList: List<String>, val sakstemaer: List<String>) : ContentRule {
    override fun skalVises(): Boolean = includeList.intersect(sakstemaer.toSet()).isNotEmpty()
}

class LastDocumentAfterContentRule(
    val sakstemakode: String,
    val periodInWeeks: Int,
    val safDokumenter: List<SafDokument>
) :
    ContentRule {
    override fun skalVises(): Boolean =
        safDokumenter.none {
            it.sakstemakode == sakstemakode && it.sistEndret.isBefore(
                LocalDateTime.now().minusWeeks(periodInWeeks.toLong() + 1)
            )
        }
}
