package no.nav.tms.mikrofrontend.selector.metrics

import io.prometheus.client.Counter

private const val METRIC_NAME = "tms_mikrofrontend_selector_changed"

class MicrofrontendCounter {
    private val counter = Counter.build()
        .name(METRIC_NAME)
        .help("Endringer i mikrofrontender på min side")
        .labelNames("action", "microfrontendId")
        .register()

    fun countMicrofrontendEnabled(actionMetricsType: ActionMetricsType, microfrontendId: String) {
        counter
            .labels(actionMetricsType.name, microfrontendId)
            .inc()
    }

}
enum class ActionMetricsType { ENABLE, DISABLE }
