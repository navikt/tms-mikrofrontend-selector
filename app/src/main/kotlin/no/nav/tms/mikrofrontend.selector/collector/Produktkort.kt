package no.nav.tms.mikrofrontend.selector.collector


abstract class ProduktkortRegel {
    abstract fun applyRule(): Boolean
}

class Produktkort(
    val id: String,
    val navn: String,
    val rules: List<ProduktkortRegel> = emptyList()
) {
    fun skalVises() =
        rules.all { it.applyRule() }
}

val sykefravær = Produktkort(id = "PEN", navn = "Pensjon")

internal val produktkortene = mapOf(
    "DAG" to Produktkort(id = "DAG", navn = "Dagpenger"),
    "FOR" to Produktkort(id = "FOR", navn = "Foreldrepenger"),
    "HJE" to Produktkort(id = "HJE", navn = "Hjelpemidler"),
    "KOM" to Produktkort(id = "KOM", navn = "Sosialhjelp"),
    "PEN" to Produktkort(id = "PEN", navn = "Pensjon"),
    "SYK" to sykefravær,
    "SYM" to sykefravær,
    "UFO" to Produktkort(id = "UFO", navn = "Uføretrygd")
)