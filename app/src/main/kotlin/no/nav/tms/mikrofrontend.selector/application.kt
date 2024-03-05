package no.nav.tms.mikrofrontend.selector

import io.ktor.client.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage

fun main() {
    val environment = Environment()

    startRapid(
        environment = environment,
        manifestStorage = ManifestsStorage(environment.initGcpStorage(), environment.storageBucketName)
    )
}

private fun startRapid(
    environment: Environment,
    manifestStorage: ManifestsStorage
) {
    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        counter = MicrofrontendCounter()
    )
    RapidApplication.Builder(fromEnv(environment.rapidConfig()))
        .withKtorModule {
            val apiClient = HttpClient {
                configureJackson()
            }
            selectorApi(PersonalContentCollector(apiClient, repository = personRepository, manifestStorage))
        }
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