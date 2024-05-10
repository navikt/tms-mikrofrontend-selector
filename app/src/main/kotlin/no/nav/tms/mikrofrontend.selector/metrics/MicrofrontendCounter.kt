package no.nav.tms.mikrofrontend.selector.metrics

import io.prometheus.client.Counter

const val METRIC_PREFIX = "tms_mikrofrontend_selector"

class MicrofrontendCounter {
    private val counter = Counter.build()
        .name("${METRIC_PREFIX}_changed")
        .help("Endringer i mikrofrontender på min side")
        .labelNames("action", "microfrontendId")
        .register()

    fun countMicrofrontendActions(actionMetricsType: ActionMetricsType, microfrontendId: String) {
        counter
            .labels(actionMetricsType.name, microfrontendId)
            .inc()
    }
}

enum class ActionMetricsType { ENABLE, DISABLE }