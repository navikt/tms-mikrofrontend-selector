package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.versions.LevelOfAssuranceResolver.fromJsonNode
import no.nav.tms.mikrofrontend.selector.versions.LevelOfAssuranceResolver.fromSikkerhetsnivå
import no.nav.tms.mikrofrontend.selector.versions.LevelOfAssuranceResolver.fromString
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.HIGH
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance.SUBSTANTIAL
import org.junit.jupiter.api.Test

internal class SensitivitetTest {

    private val objectmapper = jacksonObjectMapper { }

    @Test
    fun `konverterer fra sikkehetsnivå til level of assurance`() {
        fromSikkerhetsnivå(0) shouldBe HIGH
        fromSikkerhetsnivå(3) shouldBe SUBSTANTIAL
        fromSikkerhetsnivå(4) shouldBe HIGH
        fromSikkerhetsnivå(2) shouldBe HIGH
        fromSikkerhetsnivå(8) shouldBe HIGH
    }

    @Test
    fun `konverterer fra string til level of assurance`() {
        fromString("high") shouldBe HIGH
        fromString("HIGH") shouldBe HIGH
        fromString("SUBSTANTIAL") shouldBe SUBSTANTIAL
        fromString("substantial") shouldBe SUBSTANTIAL
        fromString("ukjent nivå") shouldBe HIGH
    }

    @Test
    fun `konverterer fra jsonnode til level of assurance`() {
        fromJsonNode(sensitivitetJson("4")["sensitivitet"]) shouldBe HIGH
        fromJsonNode(sensitivitetJson("3")["sensitivitet"]) shouldBe SUBSTANTIAL
        fromJsonNode(sensitivitetJson("substantial")["sensitivitet"]) shouldBe SUBSTANTIAL
        fromJsonNode(sensitivitetJson("high")["sensitivitet"]) shouldBe HIGH
        fromJsonNode(sensitivitetJson("unknown")["sensitivitet"]) shouldBe HIGH
        fromJsonNode(objectmapper.readTree("{}")) shouldBe HIGH
    }

    fun sensitivitetJson(value: String) = """{"sensitivitet":"$value"}""".trimMargin().let { objectmapper.readTree(it) }

}