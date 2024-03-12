package no.nav.tms.mikrofrontend.selector.collector

import no.nav.tms.mikrofrontend.selector.database.Microfrontends


abstract class ProduktkortRegel {
    abstract fun applyRule(ident: String, microfrontends: List<String>): Boolean
}

class Produktkort(
    val id: String,
    val navn: String,
    val rules: List<ProduktkortRegel> = emptyList()
) {

    fun skalVises(ident: String, microfrontends: List<String>) =
        rules.all { it.applyRule(ident, microfrontends) }

    companion object {
        fun List<Produktkort>.ids() = this.map { it.id }
    }
}

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
        fun resolveProduktkort(koder: List<String>, ident: String, microfrontends: Microfrontends?): List<Produktkort> =
            koder.mapNotNull { kode ->
                if (values.contains(kode)) {
                    ProduktkortVerdier.valueOf(kode).produktkort
                        .let {
                            if (it.skalVises(ident, microfrontends?.ids() ?: emptyList())) {
                                it
                            } else null
                        }
                } else null
            }.distinctBy { it.id }
    }
}

