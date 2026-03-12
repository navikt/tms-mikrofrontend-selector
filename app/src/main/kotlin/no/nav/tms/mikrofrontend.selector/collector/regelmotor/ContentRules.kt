package no.nav.tms.mikrofrontend.selector.collector.regelmotor


import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import java.time.LocalDateTime

data class RuleContext(
    val sakstemaKoder: List<String>,
    val dokumenter: List<Dokument>,
    val alder: Int?,
    val levelOfAssurance: LevelOfAssurance
)

interface ContentRule {
    fun skalVises(context: RuleContext): Boolean
}

class ContentRulesDefinition(
    val id: String,
    val includeIfSakstema: IncludeIfSakstemaContentRule?,
    val excludeIfSakstema: ExcludeIfSakstemaContentRule?,
    val usersAgeOver: UsersAgeOverContentRule?,
    val weeksSinceLastDocument: WeeksSinceLastDocumentContentRule?,
    val includeOnlyIfLoAIsHigh: IncludeOnlyIfLoAIsHighRule?
) {

    private val allRules: List<ContentRule> = listOfNotNull(
        includeIfSakstema,
        excludeIfSakstema,
        usersAgeOver,
        weeksSinceLastDocument,
        includeOnlyIfLoAIsHigh
    )

    fun skalVises(context: RuleContext): Boolean = allRules.all { it.skalVises(context) }

    companion object {
        fun JsonNode.initContentRules(category: String, requireSakstemakode: Boolean) =
            this[category].toList().map {
                ContentRulesDefinition(
                    id = it.read<String>("$.id") ?: throw IllegalArgumentException("contentrules må ha en id"),
                    includeIfSakstema = IncludeIfSakstemaContentRule.parseRuleOrNull(it, requireSakstemakode, category),
                    weeksSinceLastDocument = WeeksSinceLastDocumentContentRule.parseRuleOrNull(it),
                    excludeIfSakstema = ExcludeIfSakstemaContentRule.parseRuleOrNull(it),
                    usersAgeOver = UsersAgeOverContentRule.parseRuleOrNull(it),
                    includeOnlyIfLoAIsHigh = IncludeOnlyIfLoAIsHighRule.parseRuleOrNull(it)
                )
            }
    }
}

class UsersAgeOverContentRule(private val shouldBeOlderThan: Int) : ContentRule {
    override fun skalVises(context: RuleContext): Boolean =
        context.alder?.let { it > shouldBeOlderThan } ?: true

    companion object {
        fun parseRuleOrNull(jsonNode: JsonNode) = jsonNode.read<Int>("$.usersAgeOver")?.let {
            UsersAgeOverContentRule(it)
        }
    }
}


class ExcludeIfSakstemaContentRule(private val excludeList: List<String>) : ContentRule {
    override fun skalVises(context: RuleContext): Boolean =
        excludeList.intersect(context.sakstemaKoder.toSet()).isEmpty()

    companion object {
        fun parseRuleOrNull(it: JsonNode?): ExcludeIfSakstemaContentRule? =
            it?.read<List<String>>("$.excludeIfSakstema")?.let { sakstema ->
                ExcludeIfSakstemaContentRule(sakstema)
            }
    }
}

class IncludeIfSakstemaContentRule(private val includeList: List<String>) : ContentRule {

    override fun skalVises(context: RuleContext): Boolean =
        includeList.intersect(context.sakstemaKoder.toSet()).isNotEmpty()

    companion object {
        const val ruleId = "includeIfSakstema"
        fun parseRuleOrNull(jsonNode: JsonNode, requireSakstemakode: Boolean, category: String) =
            jsonNode.read<List<String>>("$.$ruleId").let { sakstema ->
                if (requireSakstemakode && sakstema == null) {
                    throw IllegalArgumentException("sakstemakoder er påkrevd for innhold av type $category")
                } else sakstema
            }?.let { IncludeIfSakstemaContentRule(it) }
    }
}


class WeeksSinceLastDocumentContentRule(
    private val sakstemakode: String,
    private val periodInWeeks: Int,
) : ContentRule {
    override fun skalVises(context: RuleContext): Boolean =
        context.dokumenter.any {
            it.kode == sakstemakode && it.sistEndret.isAfter(
                LocalDateTime.now().minusWeeks(periodInWeeks.toLong()).minusDays(1)
            )
        }

    companion object {
        const val ruleId = "weeksSinceLastDocument"
        fun parseRuleOrNull(jsonNode: JsonNode) = jsonNode.read<JsonNode>("$.$ruleId")?.let {
            WeeksSinceLastDocumentContentRule(
                periodInWeeks = it.read<Int>("$.numberOfWeeks")
                    ?: throw IllegalArgumentException("Antall uker må være definert for ukerEtterSisteDokumentRegler"),
                sakstemakode = it.read<String>("$.sakstemakode")
                    ?: throw IllegalArgumentException("kode må være definert for ukerEtterSisteDokumentRegler"),
            )
        }
    }
}

class IncludeOnlyIfLoAIsHighRule(private val requireHighLevelOfAssurance: Boolean) : ContentRule {
    override fun skalVises(context: RuleContext): Boolean =
        !requireHighLevelOfAssurance || context.levelOfAssurance == LevelOfAssurance.HIGH

    companion object {
        const val ruleId = "includeOnlyIfLoAIsHigh"
        fun parseRuleOrNull(jsonNode: JsonNode) = jsonNode.read<Boolean>("$.$ruleId")?.let {
            IncludeOnlyIfLoAIsHighRule(it)
        }
    }
}
