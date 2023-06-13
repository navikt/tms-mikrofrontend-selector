package no.nav.tms.mikrofrontend.selector.database

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.*
import org.junit.jupiter.api.Test

internal class SensitivitetTest {

    @Test
    fun `resolver sikkerhetsnivå riktig`() {
        Sensitivitet.resolve(null) shouldBe HIGH
        Sensitivitet.resolve(3) shouldBe SUBSTANTIAL
        Sensitivitet.resolve(4) shouldBe HIGH
        Sensitivitet.resolve(2) shouldBe HIGH
        Sensitivitet.resolve(8) shouldBe HIGH
    }

    @Test
    fun `vurderer riktig på sensitivitet av innhold`() {
        (HIGH >= 4) shouldBe true
        (HIGH >= 3) shouldBe true
        (SUBSTANTIAL >= 4) shouldBe false
        (SUBSTANTIAL >= 3) shouldBe true
    }

}