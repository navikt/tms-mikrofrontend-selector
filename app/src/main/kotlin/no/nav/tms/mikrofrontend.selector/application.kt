package no.nav.tms.mikrofrontend.selector

import io.ktor.client.*
import no.nav.tms.kafka.application.Domain
import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.ExternalContentFetcher
import no.nav.tms.mikrofrontend.selector.collector.PdlConsumer
import no.nav.tms.mikrofrontend.selector.collector.SafTemaFetcher
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher
import no.nav.tms.mikrofrontend.selector.collector.aktuelt.AktueltCollector
import no.nav.tms.mikrofrontend.selector.database.Flyway
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.database.PostgresDatabase
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.initiatedBy
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.user.token.exchange.UserTokenExchangerBuilder

fun main() {
    val environment = Environment()

    val personRepository = PersonRepository(
        database = PostgresDatabase(environment),
        counter = MicrofrontendCounter()
    )

    val httpClient = HttpClient { configureClient() }

    val pdlConsumer = PdlConsumer(
        httpClient = httpClient,
        pdlApiUrl = environment.pdlApiUrl,
        behandlingsNummer = environment.pdlBehandlingsnummer,
    )

    val safTemaFetcher = SafTemaFetcher(
        httpClient = httpClient,
        safUrl = environment.safUrl,
        dokumentarkivUrl = environment.dokumentArkivUrl
    )

    val externalContentFetcher = ExternalContentFetcher(
        httpClient = httpClient,
        meldekortApiUrl = environment.meldekortApiUrl,
        dpMeldekortUrl = environment.dpMeldekortUrl,
        digisosUrl = environment.digisosUrl,
        sosialHjelpInnsynUrl = environment.sosialhjelpInnsynUrl,
        pdlConsumer = pdlConsumer,
        safTemaFetcher = safTemaFetcher,
        tokenFetcher = TokenFetcher(
            tokendingsService = UserTokenExchangerBuilder.build(),
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
        externalContentFetcher = externalContentFetcher
    )
}

private fun startApplication(
    environment: Environment,
    manifestStorage: ManifestsStorage,
    personRepository: PersonRepository,
    externalContentFetcher: ExternalContentFetcher,
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
                    externalContentFetcher = externalContentFetcher,
                    produktkortCounter = ProduktkortCounter()
                ),
                AktueltCollector(
                    repository = personRepository,
                    manifestStorage = manifestStorage,
                    externalContentFetcher = externalContentFetcher
                )
            )
        }

        subscribers (EnableSubscriber(personRepository), DisableSubscriber(personRepository))

        onStartup {
            Flyway.runFlywayMigrations(environment)
        }

        minSideMdc {
            domain = Domain.microfrontend
            idFieldName = "microfrontend_id"
            producedBySupplier { it.initiatedBy }
        }
    }.start()
}
