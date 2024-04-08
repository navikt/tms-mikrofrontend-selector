package no.nav.tms.mikrofrontend.selector.collector

import io.kotest.matchers.shouldBe
import nav.no.tms.common.testutils.assert
import org.junit.jupiter.api.Test

class ResponseWithErrorsTest {


    @Test
    fun `lager errorresponse med kun defaultparametre`(){
        class DefaultErros(errors:String):ResponseWithErrors(errors = errors){
            override val source: String = "default"
        }
        class DefaultNullableErros(errors:String?):ResponseWithErrors(errors = errors){
            override val source: String = "default"
        }

        ResponseWithErrors.errorInJsonResponse<DefaultErros>("").assert {
            this.errorMessage() shouldBe "Kall til default feiler: responsbody inneholder ikke json: "
        }
        ResponseWithErrors.errorInJsonResponse<DefaultNullableErros>("").assert {
            this.errorMessage() shouldBe "Kall til default feiler: responsbody inneholder ikke json: "
        }
    }

    @Test
    fun `lager errorresponse med errors som liste`(){
        class ListErros(errors:List<String>):ResponseWithErrors(errors = errors.joinToString { it }){
            override val source: String = "default"
        }
        class NullableListErros(errors:List<String>?):ResponseWithErrors(errors = errors?.joinToString { it }){
            override val source: String = "default"
        }

        ResponseWithErrors.errorInJsonResponse<ListErros>("somethin stupid").assert {
            this.errorMessage() shouldBe "Kall til default feiler: responsbody inneholder ikke json: somethin stupid"
        }
        ResponseWithErrors.errorInJsonResponse<NullableListErros>("somethin stupid").assert {
            this.errorMessage() shouldBe "Kall til default feiler: responsbody inneholder ikke json: somethin stupid"
        }
    }

}