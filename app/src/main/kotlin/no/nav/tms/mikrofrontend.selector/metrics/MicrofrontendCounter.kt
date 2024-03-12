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

class MessageVersionCounter {

    private val counter = Counter.build()
        .name("${METRIC_PREFIX}_message_version")
        .help("Versjoner av kafkameldinger som er i bruk på microfontend-topicet")
        .labelNames("version", "microfrontendId", "team")
        .register()

    fun countMessageVersion(version: String="NA", microfrontendId: String, team: String="NA") {
        counter
            .labels(version, microfrontendId, team)
            .inc()
    }

}