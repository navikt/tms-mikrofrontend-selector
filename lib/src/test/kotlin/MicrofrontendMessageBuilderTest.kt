package no.nav.tms.microfrontend.message.builder


import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import no.nav.tms.microfrontend.Sensitivitet
import org.junit.jupiter.api.Test


internal class MicrofrontendMessageBuilderTest {

    private val expectedIdent = "12345678910"
    private val expectedInitiatedBy = "team-something"
    private val expectedMicrofrontendId = "mkf3"

    @Test
    fun `disable med config objekt`() {
        shouldThrow<IllegalArgumentException> {
            MicrofrontendMessageBuilder.disable {
                ident = expectedIdent
                initiatedBy = expectedInitiatedBy
            }.map()
        }

        shouldThrow<IllegalArgumentException> {
            MicrofrontendMessageBuilder.disable {
                ident = "ugyldigidentmedbokstaver"
                initiatedBy = expectedInitiatedBy
                microfrontendId = expectedMicrofrontendId
            }.text()
        }

        shouldThrow<IllegalArgumentException> {
            MicrofrontendMessageBuilder.disable {
                ident = "112345678910"
                initiatedBy = expectedInitiatedBy
                microfrontendId = expectedMicrofrontendId
            }.jsonNode()
        }


        MicrofrontendMessageBuilder.disable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
        }.apply {
            map().run {
                this["@action"] shouldBe "disable"
            }
            jsonNode().run {
                this["@action"].asText() shouldBe "disable"
                assertCommonJsonFields(jsonNode())
            }
            assertDisableText(text())
        }


    }

    @Test
    fun `disable med parameter`() {
        MicrofrontendMessageBuilder.disable(
            ident = expectedIdent,
            microfrontenId = expectedMicrofrontendId,
            initiatedBy = expectedInitiatedBy
        ).apply {
            map().run {
                this["@action"] shouldBe "disable"
                assertCommonMap(this)
            }
            jsonNode().run {
                this["@action"].asText() shouldBe "disable"
                assertCommonJsonFields(jsonNode())
            }
            assertDisableText(text())
        }

    }

    @Test
    fun `enable med config objekt`() {
        shouldThrow<IllegalArgumentException> {
            MicrofrontendMessageBuilder.enable {
                initiatedBy = expectedInitiatedBy
                microfrontendId = expectedMicrofrontendId
            }.map()
        }

        MicrofrontendMessageBuilder.enable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
        }.apply {
            map().run {
                this["@action"] shouldBe "enable"
                this["sensitivitet"] shouldBe Sensitivitet.HIGH.kafkaValue
            }
            jsonNode().run {
                this["@action"].asText() shouldBe "enable"
                this["sensitivitet"].asText() shouldBe Sensitivitet.HIGH.kafkaValue
                assertCommonJsonFields(jsonNode())
            }
            assertEnableText(text(), Sensitivitet.HIGH.kafkaValue)
        }

        MicrofrontendMessageBuilder.enable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
        }.apply {
            map().run {
                this["@action"] shouldBe "enable"
                this["sensitivitet"] shouldBe Sensitivitet.HIGH.kafkaValue
            }
            jsonNode().run {
                this["@action"].asText() shouldBe "enable"
                this["sensitivitet"].asText() shouldBe Sensitivitet.HIGH.kafkaValue
                assertCommonJsonFields(jsonNode())
            }
            assertEnableText(text(), "high")
        }

        MicrofrontendMessageBuilder.enable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
            sensitivitet = Sensitivitet.SUBSTANTIAL
        }.apply {
            map().run {
                this["@action"] shouldBe "enable"
                this["sensitivitet"] shouldBe Sensitivitet.SUBSTANTIAL.kafkaValue
            }
            jsonNode().run {
                this["@action"].asText() shouldBe "enable"
                this["sensitivitet"].asText() shouldBe Sensitivitet.SUBSTANTIAL.kafkaValue
                assertCommonJsonFields(jsonNode())
            }
            assertEnableText(text(), "substantial")
        }


    }

    private fun assertCommonMap(map: MutableMap<String, String?>) {
        map["ident"] shouldBe expectedIdent
        map["microfrontend_id"] shouldBe expectedMicrofrontendId
        map["@initiated_by"] shouldBe expectedInitiatedBy
    }

    @Test
    fun `enable med parameter`() {
    }

    private fun assertCommonJsonFields(jsonNode: JsonNode) {
        jsonNode["@version"].asText() shouldBe "3"
        jsonNode["ident"].asText() shouldBe expectedIdent
        jsonNode["microfrontend_id"].asText() shouldBe expectedMicrofrontendId
        jsonNode["@initiated_by"].asText() shouldBe expectedInitiatedBy
    }

    private fun assertDisableText(text: String) {
        val expectedText =
            """{                
                "@version":"3",
                "@action":"disable",
                "ident":"$expectedIdent",
                "microfrontend_id":"$expectedMicrofrontendId",
                "@initiated_by":"$expectedInitiatedBy"}"""
                .replace("\\s".toRegex(), "")
        text shouldBe expectedText

    }

    private fun assertEnableText(text: String, sensitivitet: String) {
        val expectedText =
            """{
                "@version":"3",
                "@action":"enable",
                "ident":"$expectedIdent",
                "microfrontend_id":"$expectedMicrofrontendId",
                "@initiated_by":"$expectedInitiatedBy",
                "sensitivitet":"$sensitivitet" 
               }"""
                .replace("\\s".toRegex(), "")
        text shouldBe expectedText

    }
}

internal inline fun <T> T.run(block: T.() -> Unit): T =
    apply {
        block()
    }
