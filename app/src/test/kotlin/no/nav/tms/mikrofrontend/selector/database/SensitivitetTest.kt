package no.nav.tms.mikrofrontend.selector.database

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet
import no.nav.tms.mikrofrontend.selector.versions.Sensitivitet.*
import org.junit.jupiter.api.Test

internal class SensitivitetTest {

    @Test
    fun `resolver sikkerhetsnivå riktig`() {
        Sensitivitet.fromSikkerhetsnivå(0) shouldBe HIGH
        Sensitivitet.fromSikkerhetsnivå(3) shouldBe SUBSTANTIAL
        Sensitivitet.fromSikkerhetsnivå(4) shouldBe HIGH
        Sensitivitet.fromSikkerhetsnivå(2) shouldBe HIGH
        Sensitivitet.fromSikkerhetsnivå(8) shouldBe HIGH
    }

    @Test
    fun `vurderer riktig på sensitivitet av innhold`() {
        (HIGH >= 4) shouldBe true
        (HIGH >= 3) shouldBe true
        (SUBSTANTIAL >= 4) shouldBe false
        (SUBSTANTIAL >= 3) shouldBe true
    }

}