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
        ).resolverOrNull(listOf("DAG".safTestDokument(LocalDateTime.now())))?.skalVises() shouldBe true
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
        ).resolverOrNull(listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(4))))?.skalVises() shouldBe false
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
        ).resolverOrNull(listOf("DAG".safTestDokument(LocalDateTime.now().minusWeeks(3))))?.skalVises() shouldBe true
    }

    @Test
    fun `bruker er eldre enn`() {
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolverOrNull(39)?.skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolverOrNull(40)?.skalVises() shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).resolverOrNull(41)?.skalVises() shouldBe true
        UsersAgeOverContentRule(shouldBeOlderThan = 0).resolverOrNull(null) shouldBe null
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
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"))
            .resolverOrNull(listOf("DAG", "UFO"))
            ?.skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"))
            .resolverOrNull(listOf("DAG"))
            ?.skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"))
            .resolverOrNull(listOf())
            ?.skalVises() shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"))
            .resolverOrNull(listOf("SAF"))
            ?.skalVises() shouldBe true
        IncludeIfSakstemaContentRule(includeList = listOf("DAG"))
            .resolverOrNull(listOf("SAF", "SYM", "DAG"))
            ?.skalVises() shouldBe true
    }

}