package no.nav.tms.mikrofrontend.selector.database

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.versions.LevelOfAssuranceResolver.fromJsonNode
import no.nav.tms.mikrofrontend.selector.versions.LevelOfAssuranceResolver.fromSikkerhetsnivå
import no.nav.tms.mikrofrontend.selector.versions.LevelOfAssuranceResolver.fromString
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance.High
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance.Substantial
import org.junit.jupiter.api.Test

internal class SensitivitetTest {

    private val objectmapper = jacksonObjectMapper { }

    @Test
    fun `konverterer fra sikkehetsnivå til level of assurance`() {
        fromSikkerhetsnivå(0) shouldBe High
        fromSikkerhetsnivå(3) shouldBe Substantial
        fromSikkerhetsnivå(4) shouldBe High
        fromSikkerhetsnivå(2) shouldBe High
        fromSikkerhetsnivå(8) shouldBe High
    }

    @Test
    fun `konverterer fra string til level of assurance`() {
        fromString("high") shouldBe High
        fromString("High") shouldBe High
        fromString("HIGH") shouldBe High
        fromString("substantial") shouldBe Substantial
        fromString("Substantial") shouldBe Substantial
        fromString("SUBSTANTIAL") shouldBe Substantial
        fromString("ukjent nivå") shouldBe High
    }

    @Test
    fun `konverterer fra jsonnode til level of assurance`() {
        fromJsonNode(sensitivitetJson("4")["sensitivitet"]) shouldBe High
        fromJsonNode(sensitivitetJson("3")["sensitivitet"]) shouldBe Substantial
        fromJsonNode(sensitivitetJson("substantial")["sensitivitet"]) shouldBe Substantial
        fromJsonNode(sensitivitetJson("high")["sensitivitet"]) shouldBe High
        fromJsonNode(sensitivitetJson("unknown")["sensitivitet"]) shouldBe High
        fromJsonNode(objectmapper.readTree("{}")) shouldBe High
    }

    fun sensitivitetJson(value: String) = """{"sensitivitet":"$value"}""".trimMargin().let { objectmapper.readTree(it) }

}
