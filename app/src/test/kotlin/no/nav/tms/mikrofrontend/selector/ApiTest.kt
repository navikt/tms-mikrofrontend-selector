package no.nav.tms.mikrofrontend.selector

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
import no.nav.tms.mikrofrontend.selector.collector.json.JsonPathInterpreter.Companion.bodyAsNullOrJsonNode
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
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
internal class ApiTest {
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
    fun `Skal svare med liste over regelstyrte microfrontend Pensjon og kafkabaserte microfrontends`() =
        testApplication {
            val testIdent = "12345678910"
            val kafkastyrtDinOversikt = Pair("rm1", "https://cdn.test/rm1.json")
            val kafkastyrtDinOversikt2 = Pair("rm2", "https://cdn.test/rm2.json")

            initSelectorApi(testident = testIdent)
            initExternalServices(
                SafRoute(sakstemaer = listOf("SYK")),
                MeldekortRoute(harMeldekort = true),
                PdlRoute(fødselssår = 1960),
                DigisosRoute()
            )

            gcpStorage.updateManifest(mutableMapOf(kafkastyrtDinOversikt, kafkastyrtDinOversikt2))

            broadcaster.broadcastJson(
                testJsonString(
                    messageRequirements = EnableMessage,
                    ident = testIdent,
                    microfrontendId = kafkastyrtDinOversikt.first
                )
            )
            broadcaster.broadcastJson(
                testJsonString(
                    messageRequirements = EnableMessage,
                    ident = testIdent,
                    microfrontendId = kafkastyrtDinOversikt2.first
                )
            )

            client.get("/din-oversikt").let { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsNullOrJsonNode(true).run {
                    shouldNotBeNull()
                    listOrNull<JsonNode>("microfrontends").run {
                        shouldNotBeNull()
                        size shouldBe 2

                        find {
                            it["microfrontend_id"].asText() == kafkastyrtDinOversikt.first
                        }.let { microfrontend ->
                            microfrontend.shouldNotBeNull()
                            microfrontend["url"].asText() shouldBe kafkastyrtDinOversikt.second
                        }

                        find {
                            it["microfrontend_id"].asText() == kafkastyrtDinOversikt2.first
                        }.let { microfrontend ->
                            microfrontend.shouldNotBeNull()
                            microfrontend["url"].asText() shouldBe kafkastyrtDinOversikt2.second
                        }
                    }

                    listOrNull<JsonNode>("aktuelt").run {
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
    fun `Skal svare med liste over mikrofrontends og meldekort for loa-high`() = testApplication {
        val testIdent = "12345678910"
        val expectedMicrofrontends = mutableMapOf(
            "mk1" to "https://cdn.test/mk1.json",
            "mk2" to "https://cdn.test/mk2.json",
            "mk3" to "https://cdn.test/mk1.json",
        )

        initSelectorApi(testident = testIdent)
        initExternalServices(
            SafRoute(sakstemaer = listOf("DAG")),
            MeldekortRoute(harMeldekort = true),
            PdlRoute(fødselsdato = "2004-05-05", 2004),
            DigisosRoute(),
        )

        expectedMicrofrontends.keys.forEach {
            broadcaster.broadcastJson(
                testJsonString(
                    messageRequirements = EnableMessage,
                    ident = testIdent,
                    microfrontendId = it
                )
            )
        }
        broadcaster.broadcastJson(testJsonString(microfrontendId = "aia-ny", ident = testIdent))
        broadcaster.broadcastJson(testJsonString(microfrontendId = "legacyNivå4mkf", ident = testIdent))
        broadcaster.broadcastJson(
            testJsonString(
                microfrontendId = "nivå3mkf",
                ident = testIdent,
                levelOfAssurance = SUBSTANTIAL
            )
        )

        expectedMicrofrontends["nivå3mkf"] = "https://cdn.test/nivå3mkf.json"
        expectedMicrofrontends["legacyNivå4mkf"] = "https://cdn.test/legacyNivå4mkf.json"

        gcpStorage.updateManifest(expectedMicrofrontends)

        client.get("/din-oversikt").run {
            status shouldBe HttpStatusCode.OK
            bodyAsNullOrJsonNode(true).run {
                shouldNotBeNull()
                getOrNull<List<JsonNode>>("microfrontends").run {
                    shouldNotBeNull()
                    size shouldBe expectedMicrofrontends.size
                }
                getAll<String>("microfrontends..url")
                list<String>("produktkort").size shouldBe 1
                list<String>("aktuelt").size shouldBe 0
                boolean("meldekort") shouldBe true
                boolean("offerStepup") shouldBe false
            }
        }
    }

    @Test
    fun `Skal svare med liste over mikrofrontends og produktkort med nivå 4`() = testApplication {
        val testIdent = "12345678910"
        val expectedMicrofrontends = mutableMapOf(
            "mk1" to "https://cdn.test/mk1.json",
            "mk2" to "https://cdn.test/mk2.json",
            "mk3" to "https://cdn.test/mk1.json",
        )
        val expectedProduktkort = listOf("DAG", "PEN")

        initSelectorApi(testident = testIdent)
        initExternalServices(
            SafRoute(expectedProduktkort),
            MeldekortRoute(),
            PdlRoute(),
            DigisosRoute(),
        )

        expectedMicrofrontends.keys.forEach {
            broadcaster.broadcastJson(
                testJsonString(
                    messageRequirements = EnableMessage,
                    ident = testIdent,
                    microfrontendId = it
                )
            )
        }

        gcpStorage.updateManifest(expectedMicrofrontends)

        client.get("/din-oversikt").run {
            status shouldBe HttpStatusCode.OK
            bodyAsNullOrJsonNode().run {
                shouldNotBeNull()
                listOrNull<JsonNode>("microfrontends")?.size shouldBe 3
                boolean("offerStepup") shouldBe false
                list<String>("produktkort").run {

                    size shouldBe 2
                    sorted() shouldBe expectedProduktkort.sorted()
                }
            }
        }
    }

    @Test
    fun `Skal svare med liste over mikrofrontends og tom produktkortliste for ident med innloggingsnivå 3`() =
        testApplication {
            val testIdent = "12345678910"
            val nivå4Mikrofrontends = mutableMapOf(
                "mk1" to "https://cdn.test/mk1.json",
                "mk2" to "https://cdn.test/mk2.json",
                "mk3" to "https://cdn.test/mk1.json",
            )

            initSelectorApi(testident = testIdent, levelOfAssurance = SUBSTANTIAL)
            initExternalServices(
                SafRoute(),
                MeldekortRoute(),
                PdlRoute(),
                DigisosRoute(),
            )

            nivå4Mikrofrontends.keys.forEach {
                broadcaster.broadcastJson(testJsonString(microfrontendId = it, ident = testIdent))
            }
            broadcaster.broadcastJson(
                testJsonString(
                    microfrontendId = "nivå3mkf",
                    ident = testIdent,
                    levelOfAssurance = SUBSTANTIAL
                )
            )

            gcpStorage.updateManifest(
                nivå4Mikrofrontends.apply {
                    this["nivå3mkf"] = "https://nivå3mkf.cdn.test"
                }
            )

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].toList().run {
                        size shouldBe 1
                        first().run {
                            this["microfrontend_id"].asText() shouldBe "nivå3mkf"
                            this["url"].asText() shouldBe "https://nivå3mkf.cdn.test"
                        }
                    }
                    this["offerStepup"].asBoolean() shouldBe true
                    this["produktkort"].size() shouldBe 0
                }
            }
        }


    @Test
    fun `Skal svare med tom liste for personer som ikke har noen mikrofrontends eller produktkort`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(),
                MeldekortRoute(),
                PdlRoute(),
                DigisosRoute(),
            )

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].size() shouldBe 0
                    this["produktkort"].size() shouldBe 0
                    this["offerStepup"].asBoolean() shouldBe false
                }

            }
        }

    @Test
    fun `Skal svare med multistatus når saf feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"),
                MeldekortRoute(),
                DigisosRoute(),
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            broadcaster.broadcastJson(
                testJsonString(
                    microfrontendId = "nivå3mkf",
                    ident = testident2,
                    levelOfAssurance = HIGH
                )
            )

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].size() shouldBe 1
                    this["produktkort"].size() shouldBe 0
                    this["offerStepup"].asBoolean() shouldBe false
                }

            }
        }

    @Test
    fun `Svarer med 207 når eksterne tjenester feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"),
                MeldekortRoute(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                PdlRoute("2000-05-05", 2000),
                DigisosRoute(),
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            broadcaster.broadcastJson(
                testJsonString(
                    microfrontendId = "nivå3mkf",
                    ident = testident2,
                    levelOfAssurance = HIGH
                )
            )

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).run {
                    this["microfrontends"].size() shouldBe 1
                    this["produktkort"].size() shouldBe 0
                    this["aktuelt"].size() shouldBe 0
                    this["offerStepup"].asBoolean() shouldBe false
                }

            }
        }

    @Test
    fun `Retunerer ikke pensjons microfrontend når kallet til PDL feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2)
            initExternalServices(
                SafRoute(
                    errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"
                ),
                MeldekortRoute(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                PdlRoute(errorMsg = "Kall til PDL feilet"),
                DigisosRoute(),
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            broadcaster.broadcastJson(
                testJsonString(
                    microfrontendId = "nivå3mkf",
                    ident = testident2,
                    levelOfAssurance = HIGH
                )
            )

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).run {
                    this["aktuelt"].size() shouldBe 0
                }
            }
        }

    @Test
    fun `Skal returnere 207 ved SocketTimeoutException`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2, httpClient = sockettimeoutClient)

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.MultiStatus
            }
        }

    @Test
    fun `Skal retunerer 207 validering av token til andre baksystemer feiler`() =
        testApplication {
            val testident2 = "12345678910"

            initSelectorApi(testident = testident2, tokenFetcher = mockk<TokenFetcher>().apply {
                coEvery { meldekortToken(any()) } throws TokenFetcherException(
                    originalException = SocketTimeoutException(),
                    forService = "meldekort",
                    appClientId = "testid"
                )
                coEvery { safToken(any()) } returns "<saf>"
                coEvery { pdlToken(any()) } returns "<pdl>"
                coEvery { digisosToken(any()) } returns "<digisos>"
            })

            initExternalServices(
                SafRoute(),
                MeldekortRoute(),
                PdlRoute(),
                DigisosRoute(true)
            )

            client.get("/din-oversikt").run {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).run {
                    this["produktkort"].toList().run {
                        size shouldBe 1
                        first().asText() shouldBe "KOM"
                    }
                }
            }
        }

    @Test
    fun `Retunere sosialhjelp produktkort`() = testApplication {

        initSelectorApi(testident = "12345678910")
        initExternalServices(
            SafRoute(),
            MeldekortRoute(),
            PdlRoute(),
            DigisosRoute(true)
        )


        client.get("/din-oversikt").run {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText()).run {
                this["microfrontends"].size() shouldBe 0
                this["produktkort"].toList().run {
                    size shouldBe 1
                    first().asText() shouldBe "KOM"
                }
            }

        }
    }

    fun ApplicationTestBuilder.initSelectorApi(
        testident: String,
        levelOfAssurance: LevelOfAssurance = HIGH,
        httpClient: HttpClient? = null,
        tokenFetcher: TokenFetcher = mockk<TokenFetcher>().apply {
            coEvery { meldekortToken(any()) } returns "<meldekort>"
            coEvery { safToken(any()) } returns "<saf>"
            coEvery { pdlToken(any()) } returns "<pdl>"
            coEvery { digisosToken(any()) } returns "<digisos>"
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
