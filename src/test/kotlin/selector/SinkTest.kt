package selector

import LocalPostgresDatabase
import assert
import disableMessage
import enableMessage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotliquery.queryOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.mikrofrontend.selector.DisableSink
import no.nav.tms.mikrofrontend.selector.EnableSink
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ActionMetricsType
import no.nav.tms.mikrofrontend.selector.metrics.MicrofrontendCounter
import objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.IllegalArgumentException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SinkTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    private val personRepository = PersonRepository(
        database = database,
        metricsRegistry = MicrofrontendCounter(registry)
    )
    private val testRapid = TestRapid()



    @BeforeAll
    fun setupSinks() {
        EnableSink(testRapid, personRepository)
        DisableSink(testRapid, personRepository)
    }

    @AfterEach
    fun cleanDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from person") }
        registry.clear()

    }

    @Test
    fun `Skal enable ny mikrofrontend og opprette historikk`() {
        val testFnr = "12345678910"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"

        testRapid.sendTestMessage(enableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))
        testRapid.sendTestMessage(enableMessage(fnr = testFnr, microfrontendId = testmicrofeId2))
        testRapid.sendTestMessage(enableMessage(fnr = testFnr, microfrontendId = testmicrofeId2))

        personRepository.getEnabledMicrofrontends(ident = testFnr).microfrontendids().assert {
            size shouldBe 2
            this shouldContainExactly listOf(testmicrofeId1, testmicrofeId2)
        }
        database.getChangelog(testFnr).assert {
            size shouldBe 2
            get(0).assert {
                originalData shouldBe null
                newData.microfrontendids().size shouldBe 1
            }
            get(1).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 1
                newData.microfrontendids().size shouldBe 2
            }
        }

        registry.scrape().apply {
            this.getPrometheusCount(testmicrofeId1,ActionMetricsType.ENABLE) shouldBe 1
            this.getPrometheusCount(testmicrofeId2,ActionMetricsType.ENABLE) shouldBe 1
        }
    }

    @Test
    fun `Skal disable mikrofrontend og oppdatere historikk`() {
        val testFnr = "12345678910"
        val testmicrofeId1 = "new-and-shiny"
        val testmicrofeId2 = "also-new-and-shiny"

        testRapid.sendTestMessage(enableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))
        testRapid.sendTestMessage(enableMessage(fnr = "9988776655", microfrontendId = testmicrofeId1))
        testRapid.sendTestMessage(enableMessage(fnr = testFnr, microfrontendId = testmicrofeId2))

        testRapid.sendTestMessage(disableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))
        testRapid.sendTestMessage(disableMessage(fnr = testFnr, microfrontendId = testmicrofeId1))

        personRepository.getEnabledMicrofrontends(ident = testFnr).microfrontendids().assert {
            size shouldBe 1
            this shouldContainExactly listOf(testmicrofeId2)
        }
        database.getChangelog(testFnr).assert {
            size shouldBe 3
            get(0).assert {
                originalData shouldBe null
                newData.microfrontendids().size shouldBe 1
            }
            get(1).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 1
                newData.microfrontendids().size shouldBe 2
            }
            get(2).assert { originalData }.assert {
                originalData.microfrontendids().size shouldBe 2
                newData.microfrontendids().size shouldBe 1
            }
        }

        registry.scrape().apply {
            this.getPrometheusCount(testmicrofeId1,ActionMetricsType.ENABLE) shouldBe 2
            this.getPrometheusCount(testmicrofeId2,ActionMetricsType.ENABLE) shouldBe 1
            this.getPrometheusCount(testmicrofeId1,ActionMetricsType.DISABLE) shouldBe 1
        }
    }
}

private fun String?.microfrontendids(): List<String> {
    require(this != null)
    return objectMapper.readTree(this)["microfrontends"].toList().map { it.asText() }
}

private fun String.getPrometheusCount(key: String, action: ActionMetricsType): Int =
    split("\r?\n|\r".toRegex())
        .toTypedArray()
        .firstOrNull() { it.contains(key) && it.contains(action.name.lowercase()) }
        ?.let {
            it[it.length-3].digitToInt()
        }?: throw  IllegalArgumentException("fant ikke action $action for $key")