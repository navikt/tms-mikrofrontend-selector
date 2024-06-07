package no.nav.tms.mikrofrontend.selector

import LocalPostgresDatabase
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
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
import io.prometheus.client.CollectorRegistry
import no.nav.tms.common.testutils.assert
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
        CollectorRegistry.defaultRegistry.clear()
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
                SafRoute(sakstemaer = listOf("DAG"), ident = testIdent),
                MeldekortRoute(harMeldekort = true),
                OppfolgingRoute(false),
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

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.OK
                bodyAsNullOrJsonNode(true).assert {
                    require(this != null)
                    listOrNull<JsonNode>("microfrontends").assert {
                        require(this != null)
                        size shouldBe 2
                        this.find {
                            it["microfrontend_id"].asText() == kafkastyrtDinOversikt.first
                        }.assert {
                            require(this != null)
                            this["url"].asText() shouldBe kafkastyrtDinOversikt.second

                        }
                        this.find {
                            it["microfrontend_id"].asText() == kafkastyrtDinOversikt.first
                        }.assert {
                            require(this != null)
                            this["url"].asText() shouldBe kafkastyrtDinOversikt.second

                        }
                    }

                    listOrNull<JsonNode>("aktuelt").assert {
                        require(this != null)
                        size shouldBe 1
                        this.find {
                            it["microfrontend_id"].asText() == "pensjonskalkulator-microfrontend"
                        }.assert {
                            require(this != null)
                            this["url"].asText() shouldBe "https://cdn.pensjon/manifest.json"
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
            SafRoute(sakstemaer = listOf("DAG"), ident = testIdent),
            MeldekortRoute(harMeldekort = true),
            OppfolgingRoute(false),
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

        client.get("/din-oversikt").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsNullOrJsonNode(true).assert {
                require(this != null)
                getOrNull<List<JsonNode>>("microfrontends").assert {
                    require(this != null)
                    size shouldBe expectedMicrofrontends.size
                }
                getAll<String>("microfrontends..url")
                list<String>("produktkort").size shouldBe 1
                list<String>("aktuelt").size shouldBe 0
                boolean("oppfolgingContent") shouldBe false
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
            SafRoute(expectedProduktkort, ident = testIdent),
            MeldekortRoute(),
            OppfolgingRoute(false),
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

        client.get("/din-oversikt").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsNullOrJsonNode().assert {
                require(this != null)
                listOrNull<JsonNode>("microfrontends")?.size shouldBe 3
                boolean("offerStepup") shouldBe false
                list<String>("produktkort").assert {

                    size shouldBe 2
                    this.sorted() shouldBe expectedProduktkort.sorted()
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
                SafRoute(ident = testIdent),
                MeldekortRoute(),
                OppfolgingRoute(),
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

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText()).assert {
                    this["microfrontends"].toList().assert {
                        size shouldBe 1
                        first().assert {
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
                SafRoute(ident = testident2),
                MeldekortRoute(),
                OppfolgingRoute(false),
                PdlRoute(),
                DigisosRoute(),
            )

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.OK
                objectMapper.readTree(bodyAsText()).assert {
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
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999", ident = testident2),
                MeldekortRoute(),
                OppfolgingRoute(false),
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

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).assert {
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
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999", ident = testident2),
                MeldekortRoute(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                OppfolgingRoute(false, ovverideContent = ""),
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

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).assert {
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
                    errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999",
                    ident = testident2
                ),
                MeldekortRoute(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                OppfolgingRoute(false, ovverideContent = ""),
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

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).assert {
                    this["aktuelt"].size() shouldBe 0
                }
            }
        }

    @Test
    fun `Skal returnere 207 ved SocketTimeoutException`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2, httpClient = sockettimeoutClient)

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.MultiStatus
            }
        }

    @Test
    fun `Skal returnere 503 når tokendings feiler`() =
        testApplication {
            val testident2 = "12345678912"

            initSelectorApi(testident = testident2, tokenFetcher = mockk<TokenFetcher>().apply {
                coEvery { oppfolgingToken(any()) } throws TokenFetcherException(
                    originalException = SocketTimeoutException(),
                    forService = "oppfolging",
                    appClientId = "testid"
                )
                coEvery { meldekortToken(any()) } returns "<meldekort>"
                coEvery { safToken(any()) } returns "<saf>"
                coEvery { pdlToken(any()) } returns "<pdl>"
            })

            initExternalServices(
                SafRoute(),
                MeldekortRoute(),
                OppfolgingRoute(false),
                PdlRoute(),
                DigisosRoute(),
            )

            client.get("/din-oversikt").assert {
                status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

    @Test
    fun `Retunere sosialhjelp produktkort`() = testApplication {

        initSelectorApi(testident = "12345678910")
        initExternalServices(
            SafRoute(),
            MeldekortRoute(),
            OppfolgingRoute(false),
            PdlRoute(),
            DigisosRoute(true)
        )


        client.get("/din-oversikt").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText()).assert {
                this["microfrontends"].size() shouldBe 0
                this["produktkort"].toList().assert {
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
            coEvery { oppfolgingToken(any()) } returns "<oppfolging>"
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
                        oppfølgingBaseUrl = testHost,
                        meldekortUrl = testHost,
                        pdlUrl = "$testHost/pdl",
                        digisosUrl = testHost,
                        pdlBehandlingsnummer = "B000",
                        tokenFetcher = tokenFetcher
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
}
