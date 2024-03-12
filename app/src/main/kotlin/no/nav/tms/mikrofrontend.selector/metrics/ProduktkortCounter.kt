package no.nav.tms.mikrofrontend.selector.metrics

import io.prometheus.client.Counter
import no.nav.tms.mikrofrontend.selector.collector.Produktkort

class ProduktkortCounter {
    private val counter = Counter.build()
        .name("${METRIC_PREFIX}_produktkort")
        .help("Kombinasjoner av produktkort")
        .labelNames("produktkort", "antall")
        .register()

    fun countProduktkort(produktkort: List<String>) {
        if (produktkort.isNotEmpty()) {
            counter
                .labels(produktkort.joinToString(), "${produktkort.size}")
                .inc()
        }
    }
}