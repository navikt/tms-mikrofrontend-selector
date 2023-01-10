package no.nav.tms.mikrofrontend.selector

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.database.PersonRepository

fun main() {
    val environment = Environment()

    startRapid(
        environment = environment,
        personRepository = PersonRepository(PostgresDatabase(environment))
    )
}

private fun startRapid(
    environment: Environment,
    personRepository: PersonRepository,
) {
    RapidApplication.Builder(fromEnv(environment.rapidConfig())).withKtorModule {
        //api oppsett
        selectorApi(personRepository)
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
val JsonMessage.microfrontendId: String
    get() {
        return get("microfrontend_id").asText()
    }
