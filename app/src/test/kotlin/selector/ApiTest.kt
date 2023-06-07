package selector

import LocalPostgresDatabase
import assert
import enableMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import no.nav.tms.mikrofrontend.selector.selectorApi
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel
import objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiTest {

    private val testRapid = TestRapid()
    private val registry = CollectorRegistry.defaultRegistry
    private val counter = MicrofrontendCounter(registry)
    private val personRepository = PersonRepository(
        database = LocalPostgresDatabase.cleanDb(),
        metricsRegistry = counter
    )

    @BeforeAll
    fun setup() {
        registry.clear()
        EnableSink(testRapid, personRepository)
        DisableSink(testRapid, personRepository)
    }

    @Test
    fun `Skal svare med liste over mikrofrontends basert på ident`() = testApplication {
        val testIdent = "12345678910"
        val expectedMicrofrontends = mutableListOf("mk-1", "mk2", "mk3")

        application {
            selectorApi(personRepository) {
                installMockedAuthenticators {
                    installTokenXAuthMock {
                        alwaysAuthenticated = true
                        setAsDefault = true
                        staticUserPid = testIdent
                        staticSecurityLevel = SecurityLevel.LEVEL_4
                    }
                }
            }
        }

        expectedMicrofrontends.forEach {
            testRapid.sendTestMessage(enableMessage(it, testIdent,))
        }
        testRapid.sendTestMessage(enableMessage("nivå3mkf", testIdent, 3,))
        expectedMicrofrontends.add("nivå3mkf")


        client.get("/mikrofrontends").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText()).assert {
                this["microfrontends"].toList().assert {
                    size shouldBe 4
                    map { it.asText() } shouldContainExactly expectedMicrofrontends
                }
                this["offerStepup"].asBoolean() shouldBe false
            }
        }
    }

    @Test
    fun `Skal svare med liste over mikrofrontends for ident med innloggingsnivå 3`() = testApplication {
        val testIdent = "12345678910"
        val nivå4Mikrofrontends = mutableListOf("mk-1", "mk2", "mk3")

        application {
            selectorApi(personRepository) {
                installMockedAuthenticators {
                    installTokenXAuthMock {
                        alwaysAuthenticated = true
                        setAsDefault = true
                        staticUserPid = testIdent
                        staticSecurityLevel = SecurityLevel.LEVEL_3
                    }
                }
            }
        }

        nivå4Mikrofrontends.forEach {
            testRapid.sendTestMessage(enableMessage(it, testIdent,))
        }
        testRapid.sendTestMessage(enableMessage("nivå3mkf", testIdent, 3,))


        client.get("/mikrofrontends").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText()).assert {
                this["microfrontends"].toList().assert {
                    size shouldBe 1
                    first().asText() shouldBe "nivå3mkf"
                }
                this["offerStepup"].asBoolean() shouldBe true
            }
        }
    }

    @Test
    fun `Skal svare med tom liste for personer som ikke har noen mikrofrontends`() = testApplication {
        val testident2 = "12345678912"

        application {
            selectorApi(personRepository) {
                installMockedAuthenticators {
                    installTokenXAuthMock {
                        alwaysAuthenticated = true
                        setAsDefault = true
                        staticUserPid = testident2
                        staticSecurityLevel = SecurityLevel.LEVEL_4
                    }
                }
            }
        }

        client.get("/mikrofrontends").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText()).assert {
                this["microfrontends"].size() shouldBe 0
                this["offerStepup"].asBoolean() shouldBe false
            }

        }
    }

}