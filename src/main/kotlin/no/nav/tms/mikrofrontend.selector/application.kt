package no.nav.tms.mikrofrontend.selector

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter

fun main() {
    val environment = Environment()

    startRapid(
        environment = environment,
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    )
}

private fun startRapid(
    environment: Environment,
    prometheusMeterRegistry: PrometheusMeterRegistry,
) {
    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        metricsRegistry = MicrofrontendCounter(prometheusMeterRegistry)
    )
    RapidApplication.Builder(fromEnv(environment.rapidConfig())).withKtorModule {
        selectorApi(personRepository, prometheusMeterRegistry)
    }.build().apply {
        DisableSink(this, personRepository)
        EnableSink(this, personRepository)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(environment)
            }
        })
    }.start()
}

val JsonMessage.ident: String
    get() {
        return get("ident").asText()
    }
val JsonMessage.microfrontendId: String
    get() {
        return get("microfrontend_id").asText()
    }
