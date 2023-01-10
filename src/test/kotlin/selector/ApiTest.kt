package selector

import LocalPostgresDatabase
import assert
import enableMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.MicrofrontendRepository
import no.nav.tms.mikrofrontend.selector.selectorApi
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel
import objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class ApiTest{

    private val testRapid  = TestRapid()
    private val microfrontendRepository = MicrofrontendRepository(LocalPostgresDatabase.cleanDb())

    @BeforeAll
    fun setup(){
        EnableSink(testRapid,microfrontendRepository)
        DisableSink(testRapid,microfrontendRepository)
    }
    @Test
    fun `Skal svare med liste over mikrofrontends for person basert på fnr`() = testApplication {
        val testFnr1 = "12345678910"
        val expectedMicroforntends = listOf("mk-1","mk2","mk3")

        application { selectorApi(microfrontendRepository, installAuthenticatorsFunction = {
            installMockedAuthenticators {
                installTokenXAuthMock {
                    alwaysAuthenticated = true
                    setAsDefault = true
                    staticUserPid = testFnr1
                    staticSecurityLevel = SecurityLevel.LEVEL_4
                }
            }
        } ) }

        expectedMicroforntends.forEach {
            enableMessage(it,testFnr1)
        }

        client.get("/mikrofrontends").assert {
            status shouldBe HttpStatusCode.OK
            objectMapper.readTree(bodyAsText())["microfrontends"].toList().apply {
                size shouldBe 3
                map { it.asText() } shouldContainExactly expectedMicroforntends
            }
        }

        client.get("/mikrofrontends").assert {
            objectMapper.readTree(bodyAsText())["microfrontends"].toList().size shouldBe 0
        }

    }

}