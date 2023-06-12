package no.nav.tms.mikrofrontend.selector.database

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.database.Sensitivitet.*
import org.junit.jupiter.api.Test

internal class SensitivitetTest {

    @Test
    fun `resolver sikkerhetsnivå riktig`(){
        Sensitivitet.resolve(null) shouldBe HIGH
        Sensitivitet.resolve(3) shouldBe SUBSTANTIAL
        Sensitivitet.resolve(4) shouldBe HIGH
        Sensitivitet.resolve(2) shouldBe HIGH
        Sensitivitet.resolve(8) shouldBe HIGH
    }

    @Test
    fun `vurderer riktig på HIGH sensitivitet av innhold`(){
        HIGH.innholdKanVises(SUBSTANTIAL) shouldBe true
        HIGH.innholdKanVises(HIGH) shouldBe true
        HIGH.innholdKanVises("SUBSTANTIAL") shouldBe true
        HIGH.innholdKanVises("HIGH") shouldBe true
        HIGH.innholdKanVises(1) shouldBe true
        HIGH.innholdKanVises(3) shouldBe true
        HIGH.innholdKanVises(4) shouldBe true
        HIGH.innholdKanVises(8) shouldBe true
    }

    @Test
    fun `vurderer riktig på SUBSTANTIAL sensitivitet av innhold`(){
        SUBSTANTIAL.innholdKanVises(SUBSTANTIAL) shouldBe true
        SUBSTANTIAL.innholdKanVises(HIGH) shouldBe false
        SUBSTANTIAL.innholdKanVises("SUBSTANTIAL") shouldBe true
        SUBSTANTIAL.innholdKanVises("HIGH") shouldBe false
        SUBSTANTIAL.innholdKanVises(1) shouldBe false
        SUBSTANTIAL.innholdKanVises(3) shouldBe true
        SUBSTANTIAL.innholdKanVises(4) shouldBe false
        SUBSTANTIAL.innholdKanVises(8) shouldBe false
    }

}