package no.nav.tms.microfrontend.message.builder


import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tms.microfrontend.MessageBuilder
import no.nav.tms.microfrontend.Sikkerhetsnivå
import org.junit.jupiter.api.Test


internal class MessageBuilderTest {

    private val expectedIdent = "12345678910"
    private val expectedInitiatedBy = "team-something"
    private val expectedMicrofrontendId = "mkf3"

    @Test
    fun `disable med config objekt`() {
        shouldThrow<IllegalArgumentException> {
            MessageBuilder.disable {
                ident = expectedIdent
                initiatedBy = expectedInitiatedBy
            }.map()
        }

        shouldThrow<IllegalArgumentException> {
            MessageBuilder.disable {
                ident = "ugyldigidentmedbokstaver"
                initiatedBy = expectedInitiatedBy
                microfrontendId = expectedMicrofrontendId
            }.text()
        }

        shouldThrow<IllegalArgumentException> {
            MessageBuilder.disable {
                ident = "112345678910"
                initiatedBy = expectedInitiatedBy
                microfrontendId = expectedMicrofrontendId
            }.jsonNode()
        }


        MessageBuilder.disable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
        }.apply {
            map().assert {
                this["@action"] shouldBe "disable"
            }
            jsonNode().assert {
                this["@action"].asText() shouldBe "disable"
                assertCommonJsonFields(jsonNode())
            }
            assertDisableText(text())
        }


    }

    @Test
    fun `disable med parameter`() {
        MessageBuilder.disable(
            ident = expectedIdent,
            microfrontenId = expectedMicrofrontendId,
            initiatedBy = expectedInitiatedBy
        ).apply {
            map().assert {
                this["@action"] shouldBe "disable"
                assertCommonMap(this)
            }
            jsonNode().assert {
                this["@action"].asText() shouldBe "disable"
                assertCommonJsonFields(jsonNode())
            }
            assertDisableText(text())
        }

    }

    @Test
    fun `enable med config objekt`() {
        shouldThrow<IllegalArgumentException> {
            MessageBuilder.enable {
                initiatedBy = expectedInitiatedBy
                microfrontendId = expectedMicrofrontendId
            }.map()
        }

        MessageBuilder.enable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
        }.apply {
            map().assert {
                this["@action"] shouldBe "enable"
                this["sikkerhetsnivå"] shouldBe "4"
            }
            jsonNode().assert {
                this["@action"].asText() shouldBe "enable"
                this["sikkerhetsnivå"].asInt() shouldBe 4
                assertCommonJsonFields(jsonNode())
            }
            assertEnableText(text(), "4")
        }

        MessageBuilder.enable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
        }.apply {
            map().assert {
                this["@action"] shouldBe "enable"
                this["sikkerhetsnivå"] shouldBe "4"
            }
            jsonNode().assert {
                this["@action"].asText() shouldBe "enable"
                this["sikkerhetsnivå"].asInt() shouldBe 4
                assertCommonJsonFields(jsonNode())
            }
            assertEnableText(text(), "4")
        }

        MessageBuilder.enable {
            ident = expectedIdent
            initiatedBy = expectedInitiatedBy
            microfrontendId = expectedMicrofrontendId
            sikkerhetsnivå = Sikkerhetsnivå.NIVÅ_3
        }.apply {
            map().assert {
                this["@action"] shouldBe "enable"
                this["sikkerhetsnivå"] shouldBe "3"
            }
            jsonNode().assert {
                this["@action"].asText() shouldBe "enable"
                this["sikkerhetsnivå"].asInt() shouldBe 3
                assertCommonJsonFields(jsonNode())
            }
            assertEnableText(text(), "3")
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
        jsonNode["ident"].asText() shouldBe expectedIdent
        jsonNode["microfrontend_id"].asText() shouldBe expectedMicrofrontendId
        jsonNode["@initiated_by"].asText() shouldBe expectedInitiatedBy
    }

    private fun assertDisableText(text: String) {
        val expectedText =
            """{
                "@action":"disable",
                "ident":"$expectedIdent",
                "microfrontend_id":"$expectedMicrofrontendId",
                "@initiated_by":"$expectedInitiatedBy"}"""
                .replace("\\s".toRegex(), "")
        text shouldBe expectedText

    }

    private fun assertEnableText(text: String, sikkerhetsnivå: String) {
        val expectedText =
            """{
                "@action":"enable",
                "ident":"$expectedIdent",
                "microfrontend_id":"$expectedMicrofrontendId",
                "@initiated_by":"$expectedInitiatedBy",
                "sikkerhetsnivå":"$sikkerhetsnivå" 
               }"""
                .replace("\\s".toRegex(), "")
        text shouldBe expectedText

    }
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }