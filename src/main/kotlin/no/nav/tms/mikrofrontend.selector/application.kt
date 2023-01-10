package no.nav.tms.mikrofrontend.selector

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.config.Environment
import no.nav.tms.mikrofrontend.selector.config.Flyway
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.database.MicrofrontendRepository

fun main() {
    val environment = Environment()

    startRapid(
        environment = environment,
        microfrontendRepository = MicrofrontendRepository(PostgresDatabase(environment))
    )
}

private fun startRapid(
    environment: Environment,
    microfrontendRepository: MicrofrontendRepository,
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

val JsonMessage.ident: String
    get() {
        return get("ident").asText()
    }
val JsonMessage.mikrofrontendtId: String
    get() {
        return get("mikrofrontend_id").asText()
    }
