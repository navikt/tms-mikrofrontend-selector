package no.nav.tms.mikrofrontend.selector.collector

import assert
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class NullSafeJsonTest {

    @Test
    fun `skal returnere null hvis det er ugyldig json`() {
        NullSafeJson.initObjectMapper("Ikke gyldig json 12345678910") shouldBe null
    }

    @Test
    fun `skal parse gyldig json`() {
        NullSafeJson.initObjectMapper("""["onestring","twostring"]""".trimIndent()) shouldNotBe null
        NullSafeJson.initObjectMapper("""{"something":["onestring","twostring"]}""".trimIndent()) shouldNotBe null
    }

    @Test
    fun `Skal returnere null node om nøkkel ikke finnes`() {
        NullSafeJson.initObjectMapper("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            getFromPath<Boolean>("doesnotexist") shouldBe null
        }
        NullSafeJson.initObjectMapper("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            getFromPath<Boolean>("something.doesnotexist") shouldBe null
        }
        NullSafeJson.initObjectMapper("""{"something": { "nested":"tadda"} }""".trimIndent()).assert {
            require(this != null)
            getFromPath<Boolean>("something.doesnotexist") shouldBe null
        }
    }

    @Test
    fun `Skal finne eksisterende noder med komplett path`() {
        NullSafeJson.initObjectMapper(
            """
            {
                "levelOneString":"nmbr1!",
                "level_one": {
                "levelTwoList":["liststuff"],
                "levelTwoIntValue": 2,
                "levelTwoObject": {
                "level3BooelanValue":true,
                "level3List":[1,2,3,4,5,6,7,8,9,10],
                "level3Object": {
                        "level4StringValue":"Hurra!"     
                    }
                }
                }
            }
        """.trimIndent()
        ).assert {
            require(this != null)
            getFromPath<String>("levelOneString") shouldBe "nmbr1!"
            getFromPath<JsonNode>("level_one") shouldNotBe null
            getFromPath<List<String>>("level_one.levelTwoList").assert {
                require(this != null)
                size shouldBe 1
                first() shouldBe "liststuff"
            }
            getFromPath<String>("level_one.levelTwoObject.level3Object.level4StringValue") shouldBe "Hurra!"
        }
    }

    @Test
    fun `Skal finne eksisterende noder med nested key som eneste input`() {
        NullSafeJson.initObjectMapper(
            """
            {
                "levelOneString":"nmbr1!",
                "duplicateValue":"dup",
                "level_one": {
                "duplicateValue":"dup",
                "levelTwoList":["liststuff"],
                "levelTwoIntValue": 2,
                "levelTwoObject": {
                "duplicateValue":"dup",
                "level3BooelanValue":true,
                "level3List":[1,2,3,4,5,6,7,8,9,10],
                "level3Object": {
                        "level4StringValue":"Hurra!"     
                    }
                }
                }
            }
        """.trimIndent()
        ).assert {
            require(this != null)
            getFromKey<String>("levelOneString") shouldBe "nmbr1!"
            getFromKey<List<Int>>("level3List").assert {
                require(this != null)
                size shouldBe 10
                first() shouldBe 1
                last() shouldBe 10
            }
            getFromKey<String>("level4StringValue") shouldBe "Hurra!"
            getFromKey<String>("level3Object.level4StringValue") shouldBe "Hurra!"
            getAllValuesForKey<String>("duplicateValue")!!.size shouldBe 3
        }
    }

    @Test
    fun `Kaster ikke nullpointerexception`() {
        val sakstemaer = listOf("DAG", "FOR")
        val safResponse = """
        {  
          "data": {
            "dokumentoversiktSelvbetjening": {
              "tema": ${
            sakstemaer.joinToString(prefix = "[", postfix = "]") { """{ "kode": "$it" }""".trimIndent() }
        }
            }
          }
        }
    ""${'"'}.trimIndent()
""".trimIndent()
        NullSafeJson.initObjectMapper(safResponse)
            ?.getFromPath<List<String>>("data.dokumentoversiktSelvbetjening.tema..kode")
            .assert {
                require(this != null)
                this.size shouldBe 2
            }
        val arb = NullSafeJson.initObjectMapper(
            """
        {
          "erArbeidssoker": false,
          "erStandard": false
        }
    """.trimIndent()
        )

        assertDoesNotThrow { arb!!.getFromKey<Boolean>("noe") }
    }
}