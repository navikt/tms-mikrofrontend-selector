package no.nav.tms.mikrofrontend.selector

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.config.Environment
import no.nav.tms.mikrofrontend.selector.config.Flyway
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.database.MikrofrontendRepository

fun main() {
    val environment = Environment()

    startRapid(
        environment = environment,
        mikrofrontendRepository = MikrofrontendRepository(PostgresDatabase(environment))
    )
}

private fun startRapid(
    environment: Environment,
    mikrofrontendRepository: MikrofrontendRepository,
) {
    RapidApplication.Builder(fromEnv(environment.rapidConfig())).withKtorModule {
        //api oppsett
    }.build().apply {
        //rapidsoppsett
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(environment)
            }
        })
    }
}

