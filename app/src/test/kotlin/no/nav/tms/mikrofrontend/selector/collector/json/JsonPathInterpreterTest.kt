package no.nav.tms.mikrofrontend.selector.collector.json

import assert
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPathInterpreterTest {

    private val jsonNode = JsonPathInterpreter.initPathInterpreter(
        """
            {
              "levelOneString": "nmbr1!",
              "empty_list": [],
              "objectList": [
                {
                  "id": "some1"
                },
                {
                  "id": "some2"
                }
              ],
              "level_one": {
                "levelTwoList": [
                  "liststuff"
                ],
                "levelTwoIntValue": 2,
                "levelTwoObject": {
                  "level3BooelanValue": true,
                  "listWithElements": [{ "id": 2 }, { "id": 3 }, { "id": 4 }],
                  "level3List": [1,2,3,4,5,6,7,8,9,10],
                  "level3Object": {
                    "level4StringValue": "Hurra!"
                  }
                }
              }
            }
        """.trimIndent(),
        true
    )

    @Test
    fun `Instansierer objekt fra string med jsoninnhold`() {
        JsonPathInterpreter.initPathInterpreter("Ikke gyldig json 12345678910") shouldBe null
        JsonPathInterpreter.initPathInterpreter("") shouldBe null
        JsonPathInterpreter.initPathInterpreter("{}")?.hasContent() shouldBe false
        JsonPathInterpreter.initPathInterpreter("""["onestring","twostring"]""".trimIndent()) shouldNotBe null
        JsonPathInterpreter.initPathInterpreter("""{"something":["onestring","twostring"]}""".trimIndent()) shouldNotBe null
    }

    @Test
    fun `Skal returnere null node om nøkkel ikke finnes`() {
        JsonPathInterpreter.initPathInterpreter("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            booleanOrNull("doesnotexist") shouldBe null
        }
        JsonPathInterpreter.initPathInterpreter("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            stringOrNull("doesnotexist") shouldBe null
        }
        JsonPathInterpreter.initPathInterpreter("""{"something":["onestring","twostring"]}""".trimIndent()).assert {
            require(this != null)
            booleanOrNull("something.doesnotexist") shouldBe null
        }
        JsonPathInterpreter.initPathInterpreter("""{"something": { "nested":"tadda"} }""".trimIndent()).assert {
            require(this != null)
            booleanOrNull("something.doesnotexist") shouldBe null
        }
    }

    @Test
    fun `Skal finne eksisterende noder med komplett path`() {
        jsonNode.assert {
            require(this != null)
            string("levelOneString") shouldBe "nmbr1!"
            assertThrows<JsonPathSearchException> { string("level_one") }
            listOrNull<String>("level_one.levelTwoList").assert {
                require(this != null)
                size shouldBe 1
                first() shouldBe "liststuff"
            }
            listOrNull<Int>("level_one.levelTwoObject.level3List")
            string("level_one.levelTwoObject.level3Object.level4StringValue") shouldBe "Hurra!"
            list<String>("empty_list") shouldBe emptyList()
            string("level4StringValue") shouldBe "Hurra!"
            string("level3Object.level4StringValue") shouldBe "Hurra!"
            getAll<Int>("listWithElements").assert {
                size shouldBe 1
                first().size shouldBe 3
            }
        }
    }

    @Test
    fun `Skal finne eksisterende noder med nested key som eneste input`() {
        jsonNode.assert {
            require(this != null)
            string("levelOneString") shouldBe "nmbr1!"
            listOrNull<Int>("level3List").assert {
                require(this != null)
                size shouldBe 10
                first() shouldBe 1
                last() shouldBe 10
            }
            string("level4StringValue") shouldBe "Hurra!"
            string("level3Object.level4StringValue") shouldBe "Hurra!"
        }
    }

    @Test
    fun `bruker riktig funksjon for kun nøkkelpath`() {
        jsonNode.assert {
            require(this != null)
            string("levelOneString") shouldBe "nmbr1!"
            listOrNull<Int>("level3List").assert {
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
                string("listWithElements..id")
            }
            getAll<Int>("listWithElements..id").assert {
                size shouldBe 1
                first() shouldBe listOf(1, 2, 3)
            }
            assertThrows<JsonPathSearchException> { string("notakey") }
            stringOrNull("notakey") shouldBe null
        }
    }

}
