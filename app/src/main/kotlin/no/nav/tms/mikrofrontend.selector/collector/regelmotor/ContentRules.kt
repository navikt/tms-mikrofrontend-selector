package no.nav.tms.mikrofrontend.selector.collector.regelmotor


import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument
import java.time.LocalDateTime

interface ContentResolver {
    fun skalVises(): Boolean
}

interface ContentRule<I> {
    fun resolverOrNull(input: I): ContentResolver?
}

class ContentRulesFactory(
    val id: String,
    val includeIfSakstema: IncludeIfSakstemaContentRule?,
    val exludeIfSakstema: ExcludeIfSakstemaContentRule?,
    val usersAgeOver: UsersAgeOverContentRule?,
    val weeksSinceLastDocument: WeeksSinceLastDocumentContentResolver.WeeksAndSakstema?
) {

    fun createRules(safDokumenter: List<SafDokument>, alder: Int?) = mutableListOf<ContentResolver>().apply {
        includeIfSakstema?.resolverOrNull(safDokumenter.map { dok -> dok.sakstemakode })
        exludeIfSakstema?.resolverOrNull(safDokumenter.map { dok -> dok.sakstemakode })
        usersAgeOver?.resolverOrNull(alder)?.let { add(it) }

        addIfNotNull(weeksSinceLastDocument) {
            WeeksSinceLastDocumentContentResolver(
                sakstemakode = it.sakstemakode,
                periodInWeeks = it.antallUker,
                safDokumenter = safDokumenter
            )
        }
    }

    private fun <T> MutableList<ContentResolver>.addIfNotNull(value: T?, construct: (T) -> ContentResolver) {
        if (value != null) {
            add(construct(value))
        }
    }

    companion object {
        fun JsonNode.initContentRuleFactory(category: String, requireSakstemakode: Boolean) =
            this[category].toList().map {
                ContentRulesFactory(
                    id = it.read<String>("$.id") ?: throw IllegalArgumentException("contentrules må ha en id"),
                    includeIfSakstema = IncludeIfSakstemaContentRule.getSakstemaIfDefined(
                        it,
                        requireSakstemakode,
                        category
                    ),
                    weeksSinceLastDocument = WeeksSinceLastDocumentContentResolver.getParamsIfRuleDefined(it),
                    exludeIfSakstema = ExcludeIfSakstemaContentRule.parseRuleOrNull(it),
                    usersAgeOver = UsersAgeOverContentRule.parseRuleOrNull(it)
                )
            }
    }
}

class UsersAgeOverContentRule(val shouldBeOlderThan: Int?) : ContentRule<Int?> {
    override fun resolverOrNull(input: Int?): ContentResolver? = shouldBeOlderThan?.let {
        input?.let {
            object : ContentResolver {
                override fun skalVises(): Boolean = input > shouldBeOlderThan
            }
        }
    }

    companion object {
        fun parseRuleOrNull(jsonNode: JsonNode) =
            UsersAgeOverContentRule(jsonNode.read<Int>("$.usersAgeOver"))
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

open class IncludeIfSakstemaContentRule(val includeList: List<String>?) :
    ContentRule<List<String>> {


    companion object {
        const val ruleId = "includeIfSakstema"
        fun getSakstemaIfDefined(jsonNode: JsonNode, requireSakstemakode: Boolean, category: String) =
            jsonNode.read<List<String>>("$.$ruleId").let { sakstema ->
                if (requireSakstemakode && sakstema == null) {
                    throw IllegalArgumentException("sakstemakoder er påkrevd for innhold av type $category")
                } else sakstema
            }
    }

    override fun resolverOrNull(input: List<String>): ContentResolver? =
        includeList?.let {
            object : ContentResolver {
                override fun skalVises(): Boolean = includeList.intersect(input.toSet()).isNotEmpty()
            }
        }

}


class WeeksSinceLastDocumentContentResolver(
    val sakstemakode: String,
    val periodInWeeks: Int,
) : ContentRule<List<SafDokument>> {


    companion object {
        const val ruleId = "weeksSinceLastDocument"
        fun getParamsIfRuleDefined(jsonNode: JsonNode) = jsonNode.read<JsonNode>("$.$ruleId")?.let {
            WeeksAndSakstema(
                it.read<Int>("$.antallUker")
                    ?: throw IllegalArgumentException("Antall uker må være definert for ukerEtterSisteDokumentRegler"),
                it.read<String>("$.kode")
                    ?: throw IllegalArgumentException("kode må være definert for ukerEtterSisteDokumentRegler"),
            )
        }
    }

    class WeeksAndSakstema(val antallUker: Int, val sakstemakode: String)

    override fun resolverOrNull(input: List<SafDokument>): ContentResolver? {
        object: ContentResolver {
            override fun skalVises(): Boolean =
                input.none {
                    it.sakstemakode == sakstemakode && it.sistEndret.isBefore(
                        LocalDateTime.now().minusWeeks(periodInWeeks.toLong() + 1)
                    )
                }
        }
    }
}
