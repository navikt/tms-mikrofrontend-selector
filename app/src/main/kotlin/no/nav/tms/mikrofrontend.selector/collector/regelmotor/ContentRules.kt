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
    val usersAgeOver: Int?,
    val weeksSinceLastDocument: UkerEtterSisteDokument?
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
        addIfNotNull(usersAgeOver, alder) { olderThan, age ->
            UsersAgeOverContentRule(shouldBeOlderThan = olderThan, ageOfUser = age)
        }
        addIfNotNull(weeksSinceLastDocument) {
            WeeksSinceLastDocumentContentRule(
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
                    includeIfSakstema = it.read<List<String>>("$.${IncludeIfSakstemaContentRule.ruleId}").let { sakstema ->
                        if (requireSakstemakode && sakstema == null) {
                            throw IllegalArgumentException("sakstemakoder er påkrevd for innhold av type $category")
                        } else sakstema
                    },
                    weeksSinceLastDocument = it.read<JsonNode>("$.${WeeksSinceLastDocumentContentRule.ruleId}")?.let {
                        UkerEtterSisteDokument(
                            it.read<Int>("$.antallUker")
                                ?: throw IllegalArgumentException("Antall uker må være definert for ukerEtterSisteDokumentRegler"),
                            it.read<String>("$.kode")
                                ?: throw IllegalArgumentException("kode må være definert for ukerEtterSisteDokumentRegler"),
                        )
                    },
                    exludeIfSakstema = it.read<List<String>>("$.${ExcludeIfSakstemaContentRule.ruleId}"),
                    usersAgeOver = it.read<Int>("$.${UsersAgeOverContentRule.ruleId}")
                )
            }
    }

    class UkerEtterSisteDokument(val antallUker: Int, val sakstemakode: String)
}

class UsersAgeOverContentRule(val shouldBeOlderThan: Int, val ageOfUser: Int) : ContentRule {
    override fun skalVises(): Boolean = ageOfUser > shouldBeOlderThan
    companion object{
        const val ruleId = "usersAgeOver"
    }
}

class ExcludeIfSakstemaContentRule(val excludeList: List<String>, val sakstemaer: List<String>) : ContentRule {
    override fun skalVises(): Boolean = excludeList.intersect(sakstemaer.toSet()).isEmpty()
    companion object{
        const val ruleId = "excludeIfSakstema"
    }
}

open class IncludeIfSakstemaContentRule(val includeList: List<String>, val sakstemaer: List<String>) : ContentRule {
    override fun skalVises(): Boolean = includeList.intersect(sakstemaer.toSet()).isNotEmpty()
    companion object{
        const val ruleId = "includeIfSakstema"
    }
}

class WeeksSinceLastDocumentContentRule(
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

    companion object{
        const val ruleId = "weeksSinceLastDocument"
    }
}
