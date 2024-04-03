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
            safDokumenter = listOf("DAG".safTestDokument(LocalDateTime.now()))
        ).skalVises() shouldBe true
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
            safDokumenter = listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(4)))
        ).skalVises() shouldBe false
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
            safDokumenter = listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(3)))
        ).skalVises() shouldBe true
    }
    @Test
    fun `bruker er eldre enn`() {
        UsersAgeOverContentRule(shouldBeOlderThan = 40, ageOfUser = 39).skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40, ageOfUser = 40).skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40, ageOfUser = 41).skalVises() shouldBe true
    }

    @Test
    fun `Eksluderer på gitte sakstema`() {
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM"), sakstemaer = listOf("DAG", "UFO"))
            .skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf(), sakstemaer = listOf("DAG"))
            .skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf(), sakstemaer = listOf())
            .skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM"), sakstemaer = listOf("SAF"))
            .skalVises() shouldBe false
        ExcludeIfSakstemaContentRule(excludeList = listOf("DAG"), sakstemaer = listOf("SAF", "SYM", "DAG"))
            .skalVises() shouldBe false
    }

    @Test
    fun `Inkluderer på gitte sakstema`() {
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"), sakstemaer = listOf("DAG", "UFO"))
            .skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"), sakstemaer = listOf("DAG"))
            .skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"), sakstemaer = listOf())
            .skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"), sakstemaer = listOf("SAF"))
            .skalVises() shouldBe true
        IncludeIfSakstemaContentRule(includeList = listOf("DAG"), sakstemaer = listOf("SAF", "SYM", "DAG"))
            .skalVises() shouldBe true
    }

}