package no.nav.tms.mikrofrontend.selector.metrics

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

private const val METRIC_NAMESPACE = "tms.mikrofrontend.selector.v1"

class MicrofrontendCounter(private val prometheusMeterRegistry: PrometheusMeterRegistry) {

    fun countMicrofrontendEnabled(actionMetricsType: ActionMetricsType, microfrontendId: String) {
         prometheusMeterRegistry.counter("${METRIC_NAMESPACE}.microfrontend.changed", "action", actionMetricsType.name.lowercase(), "microfrontendId", microfrontendId)
            .increment()
    }

}

enum class ActionMetricsType { ENABLE, DISABLE }

fun Routing.metrics(prometheusMeterRegistry: PrometheusMeterRegistry){
    route("/metrics"){
        get {
          call.respond(prometheusMeterRegistry.scrape())
        }
    }
}