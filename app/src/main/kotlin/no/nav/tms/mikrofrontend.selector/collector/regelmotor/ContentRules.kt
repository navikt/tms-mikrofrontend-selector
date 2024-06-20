package no.nav.tms.mikrofrontend.selector.collector.regelmotor


import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.collector.Dokument
import java.time.LocalDateTime

interface ContentResolver {
    fun skalVises(): Boolean
}

interface ContentRule<I> {
    fun resolverOrNull(input: I?): ContentResolver?
}

class ContentRulesDefinition(
    val id: String,
    val includeIfSakstema: IncludeIfSakstemaContentRule?,
    val exludeIfSakstema: ExcludeIfSakstemaContentRule?,
    val usersAgeOver: UsersAgeOverContentRule?,
    val weeksSinceLastDocument: WeeksSinceLastDocumentContentRule?
) {

    fun createRules(safDokumenter: List<Dokument>, alder: Int?) = mutableListOf<ContentResolver>().apply {
        includeIfSakstema?.resolverOrNull(safDokumenter.map { dok -> dok.kode })?.let { add(it) }
        exludeIfSakstema?.resolverOrNull(safDokumenter.map { dok -> dok.kode })?.let { add(it) }
        usersAgeOver?.resolverOrNull(alder)?.let { add(it) }
        weeksSinceLastDocument?.resolverOrNull(safDokumenter)?.let { add(it) }
    }

    companion object {
        fun JsonNode.initContentRules(category: String, requireSakstemakode: Boolean) =
            this[category].toList().map {
                ContentRulesDefinition(
                    id = it.read<String>("$.id") ?: throw IllegalArgumentException("contentrules må ha en id"),
                    includeIfSakstema = IncludeIfSakstemaContentRule.parseRuleOrNull(it, requireSakstemakode, category),
                    weeksSinceLastDocument = WeeksSinceLastDocumentContentRule.parseRuleOrNull(it),
                    exludeIfSakstema = ExcludeIfSakstemaContentRule.parseRuleOrNull(it),
                    usersAgeOver = UsersAgeOverContentRule.parseRuleOrNull(it)
                )
            }
    }
}

class UsersAgeOverContentRule(val shouldBeOlderThan: Int) : ContentRule<Int?> {
    override fun resolverOrNull(input: Int?): ContentResolver? =
        input?.let {
            object : ContentResolver {
                override fun skalVises(): Boolean = input > shouldBeOlderThan
            }
        }

    companion object {
        fun parseRuleOrNull(jsonNode: JsonNode) = jsonNode.read<Int>("$.usersAgeOver")?.let {
            UsersAgeOverContentRule(it)
        }

    }
}


class ExcludeIfSakstemaContentRule(val excludeList: List<String>?) : ContentRule<List<String>?> {
    override fun resolverOrNull(input: List<String>?): ContentResolver? =
        excludeList?.let {
            input?.let {
                object : ContentResolver {
                    override fun skalVises(): Boolean = excludeList.intersect(input.toSet()).isEmpty()
                }
            }
        }

    companion object {
        fun parseRuleOrNull(it: JsonNode?): ExcludeIfSakstemaContentRule? =
            it?.read<List<String>>("$.excludeIfSakstema")?.let { sakstema ->
                ExcludeIfSakstemaContentRule(sakstema)
            }
    }
}

open class IncludeIfSakstemaContentRule(val includeList: List<String>) :
    ContentRule<List<String>> {

    override fun resolverOrNull(input: List<String>?): ContentResolver? =
        input?.let {
            object : ContentResolver {
                override fun skalVises(): Boolean = includeList.intersect(input.toSet()).isNotEmpty()
            }
        }

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
    val sakstemakode: String,
    val periodInWeeks: Int,
) : ContentRule<List<Dokument>> {
    override fun resolverOrNull(input: List<Dokument>?): ContentResolver? =
        input?.let { safDokumenter ->
            object : ContentResolver {
                override fun skalVises(): Boolean =
                    safDokumenter.none {
                        it.kode == sakstemakode && it.sistEndret.isBefore(
                            LocalDateTime.now().minusWeeks(periodInWeeks.toLong() + 1)
                        )
                    }
            }
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
