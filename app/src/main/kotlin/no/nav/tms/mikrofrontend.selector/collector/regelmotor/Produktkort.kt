package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import no.nav.tms.mikrofrontend.selector.collector.SafResponse.SafDokument

class Produktkort(
    val id: String,
    var rules: MutableList<ContentRule> = mutableListOf()
) {
    fun skalVises() =
        rules.all { it.skalVises() }
}


class ProduktkortDefinition(
    val id: String,
    val navn: String,
    val includeIfSakstema: List<String>,
    val ukerEtterSisteDokument: Int?
) {
    fun brukerHarSakstema(
        safDokumenter: List<SafDokument>
    ): Boolean =
        includeIfSakstema.intersect(safDokumenter.map { it.sakstemakode }.toSet()).isNotEmpty()
}

