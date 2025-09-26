package no.nav.tms.mikrofrontend.selector

import io.ktor.client.*
import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.ExternalContentFecther
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher
import no.nav.tms.mikrofrontend.selector.collector.aktuelt.AktueltCollector
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder

fun main() {
    val environment = Environment()
    val dokumentarkivUrlResolver = DokumentarkivUrlResolver(environment.innsynsLenker, environment.defaultInnsynLenke)

    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        counter = MicrofrontendCounter()
    )

    val externalContentFecther = ExternalContentFecther(
        safUrl = environment.safUrl,
        httpClient = HttpClient { configureClient() },
        meldekortApiUrl = environment.meldekortApiUrl,
        dpMeldekortUrl = environment.dpMeldekortUrl,
        pdlUrl = environment.pdlApiUrl,
        digisosUrl = environment.digisosUrl,
        pdlBehandlingsnummer = environment.pdlBehandlingsnummer,
        dokumentarkivUrlResolver = dokumentarkivUrlResolver,
        tokenFetcher = TokenFetcher(
            tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
            meldekortApiClientId = environment.meldekortApiClientId,
            dpMeldekortClientId = environment.dpMeldekortApiClientId,
            safClientId = environment.safClientId,
            pdlClientId = environment.pdlClientId,
            digisosClientId = environment.digisosClientId
        ),
    )

    startApplication(
        environment = environment,
        manifestStorage = ManifestsStorage(environment.initGcpStorage(), environment.storageBucketName),
        personRepository = personRepository,
        externalContentFecther = externalContentFecther
    )
}

private fun startApplication(
    environment: Environment,
    manifestStorage: ManifestsStorage,
    personRepository: PersonRepository,
    externalContentFecther: ExternalContentFecther,
) {
    KafkaApplication.build {
        kafkaConfig {
            groupId = environment.groupId
            readTopic(environment.microfrontendtopic)
            eventNameFields("@action")
        }
        ktorModule {
            selectorApi(
                PersonalContentCollector(
                    repository = personRepository,
                    manifestStorage = manifestStorage,
                    externalContentFecther = externalContentFecther,
                    produktkortCounter = ProduktkortCounter()
                ),
                AktueltCollector(
                    repository = personRepository,
                    manifestStorage = manifestStorage,
                    externalContentFecther = externalContentFecther
                )
            )
        }

        subscribers (EnableSubscriber(personRepository), DisableSubscriber(personRepository))

        onStartup {
            Flyway.runFlywayMigrations(environment)
        }
    }.start()
}
