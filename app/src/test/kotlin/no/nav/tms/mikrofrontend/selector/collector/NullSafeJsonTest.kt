package no.nav.tms.mikrofrontend.selector.collector

import assert
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class NullSafeJsonTest {

    @Test
    fun `skal returnere null hvis det er ugyldig json`() {
        NullOrJsonNode.initObjectMapper("Ikke gyldig json 12345678910") shouldBe null
    }

    @Test
    fun `skal parse gyldig json`() {
        NullOrJsonNode.initObjectMapper("""["onestring","twostring"]""".trimIndent()) shouldNotBe null
        NullOrJsonNode.initObjectMapper("""{"something":["onestring","twostring"]}""".trimIndent()) shouldNotBe null
    }

    @Test
    fun `Skal returnere null node om nøkkel ikke finnes`() {
        NullOrJsonNode.initObjectMapper("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            getFromPath<Boolean>("doesnotexist") shouldBe null
        }
        NullOrJsonNode.initObjectMapper("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            getFromPath<Boolean>("something.doesnotexist") shouldBe null
        }
        NullOrJsonNode.initObjectMapper("""{"something": { "nested":"tadda"} }""".trimIndent()).assert {
            require(this != null)
            getFromPath<Boolean>("something.doesnotexist") shouldBe null
        }
    }

    @Test
    fun `Skal finne eksisterende noder med komplett path`() {
        NullOrJsonNode.initObjectMapper(
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
        """.trimIndent(),
            true
        ).assert {
            require(this != null)
            getFromPath<String>("levelOneString") shouldBe "nmbr1!"
            getFromPath<JsonNode>("level_one") shouldNotBe null
            getFromPath<List<String>>("level_one.levelTwoList").assert {
                require(this != null)
                size shouldBe 1
                first() shouldBe "liststuff"
            }
            getAllValuesForPath<List<Int>>("level_one.levelTwoObject.level3List")
            getFromPath<String>("level_one.levelTwoObject.level3Object.level4StringValue") shouldBe "Hurra!"
        }
    }
    @Test
    fun `Skal finne eksisterende noder med nested key som eneste input`() {
        NullOrJsonNode.initObjectMapper(
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
        """.trimIndent(),
            true
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
    fun `bruker riktig funksjon for kun nøkkelpath`() {
        NullOrJsonNode.initObjectMapper(
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
            string("levelOneString") shouldBe "nmbr1!"
            getFromKey<List<Int>>("level3List").assert {
                require(this != null)
                size shouldBe 10
                first() shouldBe 1
                last() shouldBe 10
            }
            string("level4StringValue") shouldBe "Hurra!"
            stringOrNull("level4StringValue") shouldBe "Hurra!"
            string("level3Object.level4StringValue") shouldBe "Hurra!"
            stringOrNull("level3Object.level4StringValue") shouldBe "Hurra!"
            assertThrows<MultipleValuesInJsonPathSearchException> {
                string("duplicateValue")
            }
            assertThrows<JsonPathSearchException> { string("notakey")  }
            stringOrNull("notakey") shouldBe null
        }
    }
}