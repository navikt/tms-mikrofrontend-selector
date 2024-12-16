package no.nav.tms.mikrofrontend.selector.metrics

import io.prometheus.metrics.core.metrics.Counter


const val METRIC_PREFIX = "tms_mikrofrontend_selector"

class MicrofrontendCounter {
    private val counter = Counter.builder()
        .name("${METRIC_PREFIX}_changed")
        .help("Endringer i mikrofrontender på min side")
        .labelNames("action", "microfrontendId")
        .register()

    fun countMicrofrontendActions(actionMetricsType: ActionMetricsType, microfrontendId: String) {
        counter
            .labelValues(actionMetricsType.name, microfrontendId)
            .inc()
    }
}

enum class ActionMetricsType { ENABLE, DISABLE }
