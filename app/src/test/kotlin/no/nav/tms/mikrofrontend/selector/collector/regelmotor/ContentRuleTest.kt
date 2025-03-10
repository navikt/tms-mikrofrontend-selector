package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.safTestDokument
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentRuleTest {
    @Test
    fun `periode etter siste dokument`() {
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
        ).resolver(listOf("DAG".safTestDokument(LocalDateTime.now())))?.skalVises() shouldBe true
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
        ).resolver(listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(4))))?.skalVises() shouldBe false
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
        ).resolver(listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(3))))?.skalVises() shouldBe true
    }

    @Test
    fun `bruker er eldre enn`() {
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolver(39).skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolver(40).skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolver(41).skalVises() shouldBe true
        UsersAgeOverContentRule(shouldBeOlderThan = 0).resolver(null).skalVises() shouldBe true
        UsersAgeOverContentRule(shouldBeOlderThan = 30).resolver(null).skalVises() shouldBe true
    }

    @Test
    fun `Eksluderer på gitte sakstema`() {
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM")).resolver(listOf("DAG", "UFO"))
            .skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf()).resolver(listOf("DAG"))
            .skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf()).resolver(listOf())
            .skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM")).resolver(listOf("SAF"))
            .skalVises() shouldBe false
        ExcludeIfSakstemaContentRule(excludeList = listOf("DAG")).resolver(listOf("SAF", "SYM", "DAG"))
            .skalVises() shouldBe false
    }

    @Test
    fun `Inkluderer på gitte sakstema`() {
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"))
            .resolver(listOf("DAG", "UFO"))
            .skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"))
            .resolver(listOf("DAG"))
            .skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"))
            .resolver(listOf())
            .skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"))
            .resolver(listOf("SAF"))
            .skalVises() shouldBe true
        IncludeIfSakstemaContentRule(includeList = listOf("DAG"))
            .resolver(listOf("SAF", "SYM", "DAG"))
            .skalVises() shouldBe true
    }

}
