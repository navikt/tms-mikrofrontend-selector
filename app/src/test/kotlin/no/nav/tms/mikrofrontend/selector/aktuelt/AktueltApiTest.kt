package no.nav.tms.mikrofrontend.selector.aktuelt

import no.nav.tms.mikrofrontend.selector.*
import no.nav.tms.mikrofrontend.selector.objectMapper
import no.nav.tms.mikrofrontend.selector.selectorApi
import LocalPostgresDatabase
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.tms.mikrofrontend.selector.collector.ExternalContentFecther
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher.TokenFetcherException
import no.nav.tms.mikrofrontend.selector.collector.aktuelt.AktueltCollector
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.bodyAsNullOrJsonNode
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.HIGH
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.SUBSTANTIAL
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance as MockLevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.mock.tokenXMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AktueltApiTest {
    private val counter = MicrofrontendCounter()
    private val database = LocalPostgresDatabase.cleanDb()
    private val personRepository = PersonRepository(
        database = database,
        counter = counter
    )
    private val broadcaster = setupBroadcaster(personRepository)
    private val produktkortCounter = ProduktkortCounter()
    private val gcpStorage = LocalGCPStorage.instance

    @BeforeAll
    fun setup() {
        PrometheusRegistry.defaultRegistry.clear()
    }

    @AfterEach
    fun cleanup() {
        database.deleteAll()
    }

    @Test
    fun `svarer med liste over regelstyrte microfrontend pensjon`() =
        testApplication {
            val testIdent = "12345678910"

            initSelectorApi(testident = testIdent)
            initExternalServices(
                SafRoute(sakstemaer = listOf("SYK")),
                PdlRoute(fødselssår = 1960),
            )

            gcpStorage.updateManifest()

            client.get("/aktuelt").let { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsNullOrJsonNode(true).run {
                    shouldNotBeNull()

                    listOrNull<JsonNode>("microfrontends").run {
                        shouldNotBeNull()
                        size shouldBe 1
                        find {
                            it["microfrontend_id"].asText() == "pensjonskalkulator-microfrontend"
                        }.let { microfrontend ->
                            microfrontend.shouldNotBeNull()
                            microfrontend["url"].asText() shouldBe "https://cdn.pensjon/manifest.json"
                        }
                    }
                }
            }
        }


    @Test
    fun `svarer med tom liste for personer som ikke har noen microfrontends`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(),
                PdlRoute(),
            )

            gcpStorage.clearManifest()

            client.get("/aktuelt").let { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsNullOrJsonNode(true).run {
                    shouldNotBeNull()

                    listOrNull<JsonNode>("microfrontends").run {
                        println(this.toString())
                        shouldNotBeNull()
                        size shouldBe 0
                    }
                }
            }
        }

    @Test
    fun `feiler med ServiceUnavailable når saf feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"),
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            client.get("/aktuelt").run {
                status shouldBe HttpStatusCode.ServiceUnavailable
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].size() shouldBe 0
                    this["offerStepup"].asBoolean() shouldBe false
                }

            }
        }

    @Test
    fun `feiler med ServiceUnavailable når eksterne tjenester feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"),
                PdlRoute("2000-05-05", 2000),
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            client.get("/aktuelt").run {
                status shouldBe HttpStatusCode.ServiceUnavailable
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].size() shouldBe 0
                }

            }
        }

    @Test
    fun `returnerer ikke pensjons microfrontend når kallet til PDL feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"),
                PdlRoute(errorMsg = "Kall til PDL feilet"),
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            broadcaster.broadcastJson(
                testJsonString(
                    microfrontendId = "nivå3mkf",
                    ident = testident2,
                    levelOfAssurance = HIGH
                )
            )

            client.get("/aktuelt").run {
                status shouldBe HttpStatusCode.ServiceUnavailable
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].size() shouldBe 0
                }
            }
        }

    @Test
    fun `feiler med ServiceUnavailable ved SocketTimeoutException`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2, httpClient = sockettimeoutClient)

            client.get("/aktuelt").run {
                status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

    @Test
    fun `feiler med ServiceUnavailable når validering av token til andre baksystemer feiler`() =
        testApplication {
            val testident2 = "12345678910"

            initSelectorApi(testident = testident2, tokenFetcher = mockk<TokenFetcher>().apply {
                coEvery { pdlToken(any()) } throws TokenFetcherException(
                    originalException = SocketTimeoutException(),
                    forService = "pdl",
                    appClientId = "testid"
                )
                coEvery { safToken(any()) } returns "<saf>"
            })

            initExternalServices(
                SafRoute(),
                PdlRoute(),
            )

            client.get("/aktuelt").run {
                status shouldBe HttpStatusCode.ServiceUnavailable
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].toList().run {
                        size shouldBe 0
                    }
                }
            }
        }

    fun ApplicationTestBuilder.initSelectorApi(
        testident: String,
        levelOfAssurance: LevelOfAssurance = HIGH,
        httpClient: HttpClient? = null,
        tokenFetcher: TokenFetcher = mockk<TokenFetcher>().apply {
            coEvery { safToken(any()) } returns "<saf>"
            coEvery { pdlToken(any()) } returns "<pdl>"
        }
    ) {
        val apiClient = httpClient ?: createClient { configureClient() }
        application {
            selectorApi(
                personalContentCollector = PersonalContentCollector(
                    repository = personRepository,
                    manifestStorage = ManifestsStorage(gcpStorage.storage, LocalGCPStorage.testBucketName),
                    externalContentFecther = ExternalContentFecther(
                        safUrl = testHost,
                        httpClient = apiClient,
                        meldekortUrl = testHost,
                        pdlUrl = "$testHost/pdl",
                        digisosUrl = testHost,
                        pdlBehandlingsnummer = "B000",
                        tokenFetcher = tokenFetcher,
                        dokumentarkivUrlResolver = DokumentarkivUrlResolver(generellLenke = "https://www.nav.no", temaspesifikkeLenker = mapOf("DAG" to "https://www.nav.no/dokumentarkiv/dag")),
                    ),
                    produktkortCounter = produktkortCounter
                ),
                aktueltCollector = AktueltCollector(
                    repository = personRepository,
                    manifestStorage = ManifestsStorage(gcpStorage.storage, LocalGCPStorage.testBucketName),
                    externalContentFecther = ExternalContentFecther(
                        safUrl = testHost,
                        httpClient = apiClient,
                        meldekortUrl = testHost,
                        pdlUrl = "$testHost/pdl",
                        digisosUrl = testHost,
                        pdlBehandlingsnummer = "B000",
                        tokenFetcher = tokenFetcher,
                        dokumentarkivUrlResolver = DokumentarkivUrlResolver(generellLenke = "https://www.nav.no", temaspesifikkeLenker = mapOf("DAG" to "https://www.nav.no/dokumentarkiv/dag")),
                    ),
                )
            ) {
                authentication {
                    tokenXMock {
                        alwaysAuthenticated = true
                        setAsDefault = true
                        staticUserPid = testident
                        staticLevelOfAssurance = levelOfAssurance.toMockk()
                    }
                }
            }
        }
    }
}

private fun LevelOfAssurance.toMockk(): no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance?  = when {
    this == HIGH -> MockLevelOfAssurance.HIGH
    this == SUBSTANTIAL -> MockLevelOfAssurance.SUBSTANTIAL
    else -> throw IllegalArgumentException("Ukjent vedii for level of assurance; ${this.name}")
}

val mockEngine = MockEngine { _ ->
    throw (SocketTimeoutException("Error"))
}

private val sockettimeoutClient = HttpClient(mockEngine) {
    install(ContentNegotiation) {
        jackson()
    }
    install(HttpTimeout) {

    }
}
