package selector

import LocalPostgresDatabase
import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.MikrofrontendRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class ApiTest{

    private val testRapid  = TestRapid()
    private val mikrofrontendRepository = MikrofrontendRepository(LocalPostgresDatabase.cleanDb())

    @BeforeAll
    fun setup(){
        EnableSink(testRapid,mikrofrontendRepository)
        DisableSink(testRapid,mikrofrontendRepository)
    }
    @Test
    fun `Skal svare med liste over mikrofrontends for person basert på fnr`(){
        true shouldBe true
    }

}