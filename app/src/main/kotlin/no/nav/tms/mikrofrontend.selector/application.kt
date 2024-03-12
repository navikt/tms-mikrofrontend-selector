package no.nav.tms.mikrofrontend.selector

import io.ktor.client.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.SakstemaFetcher
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder

fun main() {
    val environment = Environment()
    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        counter = MicrofrontendCounter()
    )

    val sakstemaFetcher = SakstemaFetcher(
        safUrl = environment.safUrl,
        safClientId = environment.safClientId,
        httpClient = HttpClient { configureJackson() },
        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
    )

    startRapid(
        environment = environment,
        manifestStorage = ManifestsStorage(environment.initGcpStorage(), environment.storageBucketName),
        personRepository = personRepository,
        sakstemaFetcher = sakstemaFetcher
    )
}

private fun startRapid(
    environment: Environment,
    manifestStorage: ManifestsStorage,
    personRepository: PersonRepository,
    sakstemaFetcher: SakstemaFetcher
) {
    RapidApplication.Builder(fromEnv(environment.rapidConfig()))
        .withKtorModule {

            selectorApi(
                PersonalContentCollector(
                    repository = personRepository,
                    manifestStorage = manifestStorage,
                    sakstemaFetcher = sakstemaFetcher,
                    produktkortCounter = ProduktkortCounter()
                )
            )
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