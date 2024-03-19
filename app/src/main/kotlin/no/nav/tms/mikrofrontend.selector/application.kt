package no.nav.tms.mikrofrontend.selector

import io.ktor.client.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig.Companion.fromEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.ServicesFetcher
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder

fun main() {
    val environment = Environment()
    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        counter = MicrofrontendCounter()
    )

    val servicesFetcher = ServicesFetcher(
        httpClient = HttpClient { configureJackson() },
        oppfølgingBaseUrl = environment.oppfolgingUrl,
        aiaBackendUrl = environment.aiaUrl,
        meldekortUrl =environment.meldekortUrl,
        safUrl = environment.safUrl,
        pdlUrl = "TODO",
        tokenFetcher = TokenFetcher(
            tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
            meldekortClientId = environment.meldekortClientId,
            oppfølgingClientId = environment.oppfolgingClienId,
            safClientId = environment.safClientId,
            aiaClientId = environment.aiaClientId
        )
        )

    startRapid(
        environment = environment,
        manifestStorage = ManifestsStorage(environment.initGcpStorage(), environment.storageBucketName),
        personRepository = personRepository,
        servicesFetcher = servicesFetcher
    )
}

private fun startRapid(
    environment: Environment,
    manifestStorage: ManifestsStorage,
    personRepository: PersonRepository,
    servicesFetcher: ServicesFetcher
) {
    RapidApplication.Builder(fromEnv(environment.rapidConfig()))
        .withKtorModule {

            selectorApi(
                PersonalContentCollector(
                    repository = personRepository,
                    manifestStorage = manifestStorage,
                    servicesFetcher = servicesFetcher,
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