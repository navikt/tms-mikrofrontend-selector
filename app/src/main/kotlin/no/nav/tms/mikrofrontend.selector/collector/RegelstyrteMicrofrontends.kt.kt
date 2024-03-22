package no.nav.tms.mikrofrontend.selector.collector

import io.github.oshai.kotlinlogging.KotlinLogging


sealed class RegelstyrtMicrofrontend(id: String, manifestMap: Map<String, String>) {
    private val log = KotlinLogging.logger {  }
    val definition = MicrofrontendsDefinition.create(id, manifestMap).also {
        if(it==null)
            log.debug { "Fant ikke manifest for regestyrt microfrontend med id $id i $manifestMap" }

    }
    abstract fun skalVises(): Boolean

}

class Pensjon(
    manifestMap: Map<String, String>,
    val alder: Int,
    val sakstemaer: List<String>
) : RegelstyrtMicrofrontend("pensjonskalkulator-microfrontend", manifestMap) {

    override fun skalVises(): Boolean {
        return alder > 40 && sakstemaer.none { it == ProduktkortVerdier.PEN.name } && definition != null
    }
}

object Akutelt {
    fun getAktueltContent(
        alder: Int,
        sakstemaer: List<String>,
        manifest: Map<String, String>
    ): List<MicrofrontendsDefinition> =
        listOf(Pensjon(manifestMap = manifest, alder = alder, sakstemaer = sakstemaer))
            .filter { it.skalVises() }
            .mapNotNull { it.definition }
}


