package no.nav.tms.mikrofrontend.selector

import io.prometheus.client.CollectorRegistry
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
    )
}

private fun startRapid(
    environment: Environment,
) {
    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        metricsRegistry = MicrofrontendCounter()
    )
    RapidApplication.Builder(fromEnv(environment.rapidConfig()))
        .withKtorModule { selectorApi(personRepository) }
        .build().apply {
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
val JsonMessage.initiatedBy: String?
    get() = get("initiated_by").asText()