package no.nav.tms.mikrofrontend.selector

import LocalPostgresDatabase
import assert
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.collector.NullOrJsonNode.Companion.bodyAsNullOrJsonNode
import no.nav.tms.mikrofrontend.selector.collector.PersonalContentCollector
import no.nav.tms.mikrofrontend.selector.collector.ServicesFetcher
import no.nav.tms.mikrofrontend.selector.collector.TokenFetcher
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance.LEVEL_3
import no.nav.tms.token.support.tokenx.validation.mock.LevelOfAssurance.LEVEL_4
import no.nav.tms.token.support.tokenx.validation.mock.tokenXMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiTest {

    private val testRapid = TestRapid()
    private val counter = MicrofrontendCounter()
    private val database = LocalPostgresDatabase.cleanDb()
    private val personRepository = PersonRepository(
        database = database,
        counter = counter
    )
    private val gcpStorage = LocalGCPStorage.instance

    @BeforeAll
    fun setup() {
        CollectorRegistry.defaultRegistry.clear()
        EnableSink(testRapid, personRepository)
        DisableSink(testRapid, personRepository)
    }

    @AfterEach
    fun cleanup() {
        database.deleteAll()
    }

    @Test
    fun `Skal svare med liste over mikrofrontends,meldekort og manifest med for loa-high`() = testApplication {
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
            OppfolgingRoute(false),
            ArbeidsøkerRoute()
        )

        expectedMicrofrontends.keys.forEach {
            testRapid.sendTestMessage(
                currentVersionMessage(
                    messageRequirements = EnableMessage,
                    ident = testIdent,
                    microfrontendId = it
                )
            )
        }

        //legacy
        testRapid.sendTestMessage(legacyMessagev2(microfrontendId = "legacyNivå4mkf", ident = testIdent))
        testRapid.sendTestMessage(legacyMessagev2("nivå3mkf", testIdent, 3))

        expectedMicrofrontends["nivå3mkf"] = "https://cdn.test/nivå3mkf.json"
        expectedMicrofrontends["legacyNivå4mkf"] = "https://cdn.test/legacyNivå4mkf.json"

        gcpStorage.updateManifest(expectedMicrofrontends)

        client.get("/microfrontends").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsNullOrJsonNode(true).assert {
                require(this != null)
                getFromKey<List<JsonNode>>("microfrontends").assert {
                    require(this != null)
                    size shouldBe expectedMicrofrontends.size
                }
                getAllValuesForPath<String>("microfrontends..url")
                getFromKeyOrException<List<String>>("produktkort").size shouldBe 1
                boolean("aiaStandard") shouldBe false
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
            SafRoute(expectedProduktkort),
            MeldekortRoute(),
            OppfolgingRoute(false),
            ArbeidsøkerRoute()
        )

        expectedMicrofrontends.keys.forEach {
            testRapid.sendTestMessage(
                currentVersionMessage(
                    messageRequirements = EnableMessage,
                    ident = testIdent,
                    microfrontendId = it
                )
            )
        }

        gcpStorage.updateManifest(expectedMicrofrontends)

        client.get("/microfrontends").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsNullOrJsonNode().assert {
                require(this != null)
                getFromKeyOrException<List<JsonNode>>("microfrontends").size shouldBe 3
                getFromKeyOrException<Boolean>("offerStepup") shouldBe false
                getAllValuesForPath<String>("produktkort").assert {
                    require(this != null)
                    size shouldBe 2
                    this shouldBe expectedProduktkort
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

            initSelectorApi(testident = testIdent, levelOfAssurance = LEVEL_3)
            initExternalServices(
                SafRoute(),
                MeldekortRoute(),
                OppfolgingRoute(),
                ArbeidsøkerRoute()
            )

            nivå4Mikrofrontends.keys.forEach {
                testRapid.sendTestMessage(legacyMessagev2(it, testIdent))
            }
            testRapid.sendTestMessage(legacyMessagev2("nivå3mkf", testIdent, 3))

            gcpStorage.updateManifest(
                nivå4Mikrofrontends.apply {
                    this["nivå3mkf"] = "https://nivå3mkf.cdn.test"
                }
            )

            client.get("/microfrontends").assert {
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
                SafRoute(),
                MeldekortRoute(),
                OppfolgingRoute(false),
                ArbeidsøkerRoute()
            )

            client.get("/microfrontends").assert {
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
                SafRoute(errorMsg = "Fant ikke journalpost i fagarkivet. journalpostId=999999999"),
                MeldekortRoute(),
                OppfolgingRoute(false),
                ArbeidsøkerRoute()
            )

            gcpStorage.updateManifest(mutableMapOf("nivå3mkf" to "http://wottevs"))

            testRapid.sendTestMessage(legacyMessagev2("nivå3mkf", testident2, 4))

            client.get("/microfrontends").assert {
                status shouldBe HttpStatusCode.MultiStatus
                objectMapper.readTree(bodyAsText()).assert {
                    this["microfrontends"].size() shouldBe 1
                    this["produktkort"].size() shouldBe 0
                    this["offerStepup"].asBoolean() shouldBe false
                }

            }
        }


    fun ApplicationTestBuilder.initSelectorApi(
        testident: String,
        levelOfAssurance: LevelOfAssurance = LEVEL_4
    ) {
        val apiClient = createClient { configureJackson() }
        application {
            selectorApi(
                PersonalContentCollector(
                    repository = personRepository,
                    manifestStorage = ManifestsStorage(gcpStorage.storage, LocalGCPStorage.testBucketName),
                    servicesFetcher = ServicesFetcher(
                        safUrl = testHost,
                        httpClient = apiClient,
                        oppfølgingBaseUrl = testHost,
                        aiaBackendUrl = testHost,
                        meldekortUrl = testHost,
                        tokenFetcher = mockk<TokenFetcher>().apply {
                            coEvery { oppfolgingToken(any()) } returns "<oppfolging>"
                            coEvery { meldekortToken(any()) } returns "<meldekort>"
                            coEvery { safToken(any()) } returns "<saf>"
                            coEvery { aiaToken(any()) } returns "<aia>"
                        },
                    ),
                    produktkortCounter = testproduktkortCounter
                ),
            ) {
                authentication {
                    tokenXMock {
                        alwaysAuthenticated = true
                        setAsDefault = true
                        staticUserPid = testident
                        staticLevelOfAssurance = levelOfAssurance
                    }
                }
            }
        }
    }
}