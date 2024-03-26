package no.nav.tms.mikrofrontend.selector.collector.regelmotor


import java.time.LocalDateTime

interface ContentRule {
    fun skalVises(): Boolean
}

//TODO: bruke hele safdokument
class InPeriodContentRule(val ukerEtterSisteDokument: Int, val dokumentSistEndret: LocalDateTime) :
    ContentRule {
    override fun skalVises(): Boolean =
        dokumentSistEndret.isAfter(LocalDateTime.now().minusWeeks(ukerEtterSisteDokument.toLong() + 1))
}

class OlderThanContentRule(val shouldBeOlderThan: Int, val ageOfUser: Int) : ContentRule {
    override fun skalVises(): Boolean = ageOfUser > shouldBeOlderThan
}

class ExcludeIfSakstemaContentRule(val excludeList: List<String>, val sakstemaer: List<String>) : ContentRule {
    override fun skalVises(): Boolean = excludeList.intersect(sakstemaer.toSet()).isEmpty()
}

class IncludeIfSakstemaContentRule(val includeList: List<String>, val sakstemaer: List<String>) : ContentRule {
    override fun skalVises(): Boolean = includeList.intersect(sakstemaer.toSet()).isNotEmpty()
}