package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.safTestDokument
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentRuleTest {
    @Test
    fun `periode etter siste dokument`() {
        WeeksSinceLastDocumentContentResolver(
            sakstemakode = "DAG",
            periodInWeeks = 3,
            safDokumenter = listOf("DAG".safTestDokument(LocalDateTime.now()))
        ).skalVises() shouldBe true
        WeeksSinceLastDocumentContentResolver(
            sakstemakode = "DAG",
            periodInWeeks = 3,
            safDokumenter = listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(4)))
        ).skalVises() shouldBe false
        WeeksSinceLastDocumentContentResolver(
            sakstemakode = "DAG",
            periodInWeeks = 3,
            safDokumenter = listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(3)))
        ).skalVises() shouldBe true
    }
    @Test
    fun `bruker er eldre enn`() {
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolverOrNull(39)?.skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolverOrNull(40)?.skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolverOrNull(41)?.skalVises() shouldBe true
        UsersAgeOverContentRule(shouldBeOlderThan = null).resolverOrNull(40) shouldBe null
        UsersAgeOverContentRule(shouldBeOlderThan = 30).resolverOrNull(null) shouldBe null
    }

    @Test
    fun `Eksluderer på gitte sakstema`() {
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM")).resolverOrNull(listOf("DAG", "UFO"))
            ?.skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf()).resolverOrNull(listOf("DAG"))
            ?.skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf()).resolverOrNull(listOf())
            ?.skalVises() shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM")).resolverOrNull(listOf("SAF"))
            ?.skalVises() shouldBe false
        ExcludeIfSakstemaContentRule(excludeList = listOf("DAG")).resolverOrNull(listOf("SAF", "SYM", "DAG"))
            ?.skalVises() shouldBe false
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