package no.nav.tms.mikrofrontend.selector.metrics

import io.prometheus.metrics.core.metrics.Counter

class ProduktkortCounter {
    private val counter = Counter.builder()
        .name("${METRIC_PREFIX}_produktkort")
        .help("Kombinasjoner av produktkort")
        .labelNames("produktkort", "antall")
        .register()

    fun countProduktkort(produktkort: List<String>) {
        if (produktkort.isNotEmpty()) {
            counter
                .labelValues(produktkort.sorted().joinToString(), "${produktkort.size}")
                .inc()
        }
    }
}
