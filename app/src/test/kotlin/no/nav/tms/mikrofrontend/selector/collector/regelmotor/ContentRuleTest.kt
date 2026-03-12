package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.kotest.matchers.shouldBe
import no.nav.tms.mikrofrontend.selector.safTestDokument
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentRuleTest {

    private fun contextWithDokumenter(vararg dokumenter: no.nav.tms.mikrofrontend.selector.collector.Dokument) =
        RuleContext(
            sakstemaKoder = dokumenter.map { it.kode },
            dokumenter = dokumenter.toList(),
            alder = null,
            levelOfAssurance = LevelOfAssurance.HIGH
        )

    private fun contextWithSakstema(vararg koder: String) =
        RuleContext(
            sakstemaKoder = koder.toList(),
            dokumenter = emptyList(),
            alder = null,
            levelOfAssurance = LevelOfAssurance.HIGH
        )

    private fun contextWithAlder(alder: Int?) =
        RuleContext(
            sakstemaKoder = emptyList(),
            dokumenter = emptyList(),
            alder = alder,
            levelOfAssurance = LevelOfAssurance.HIGH
        )

    @Test
    fun `periode etter siste dokument`() {
        val rule = WeeksSinceLastDocumentContentRule(sakstemakode = "DAG", periodInWeeks = 3)

        rule.skalVises(contextWithDokumenter("DAG".safTestDokument(LocalDateTime.now()))) shouldBe true
        rule.skalVises(contextWithDokumenter(
            "DAG".safTestDokument(LocalDateTime.now()),
            "DAG".safTestDokument(LocalDateTime.now().minusWeeks(4))
        )) shouldBe true
        rule.skalVises(contextWithDokumenter("DAG".safTestDokument(LocalDateTime.now().minusWeeks(4)))) shouldBe false
        rule.skalVises(contextWithDokumenter("DAG".safTestDokument(LocalDateTime.now().minusWeeks(3)))) shouldBe true
    }

    @Test
    fun `skal ikke vises når det ikke finnes dokumenter`() {
        WeeksSinceLastDocumentContentRule(
            sakstemakode = "DAG",
            periodInWeeks = 3,
        ).skalVises(contextWithDokumenter()) shouldBe false
    }

    @Test
    fun `bruker er eldre enn`() {
        UsersAgeOverContentRule(shouldBeOlderThan = 40).skalVises(contextWithAlder(39)) shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).skalVises(contextWithAlder(40)) shouldBe false
        UsersAgeOverContentRule(shouldBeOlderThan = 40).skalVises(contextWithAlder(41)) shouldBe true
        UsersAgeOverContentRule(shouldBeOlderThan = 0).skalVises(contextWithAlder(null)) shouldBe true
        UsersAgeOverContentRule(shouldBeOlderThan = 30).skalVises(contextWithAlder(null)) shouldBe true
    }

    @Test
    fun `Eksluderer på gitte sakstema`() {
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM"))
            .skalVises(contextWithSakstema("DAG", "UFO")) shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf())
            .skalVises(contextWithSakstema("DAG")) shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf())
            .skalVises(contextWithSakstema()) shouldBe true
        ExcludeIfSakstemaContentRule(excludeList = listOf("SAF", "SYM"))
            .skalVises(contextWithSakstema("SAF")) shouldBe false
        ExcludeIfSakstemaContentRule(excludeList = listOf("DAG"))
            .skalVises(contextWithSakstema("SAF", "SYM", "DAG")) shouldBe false
    }

    @Test
    fun `Inkluderer på gitte sakstema`() {
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"))
            .skalVises(contextWithSakstema("DAG", "UFO")) shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"))
            .skalVises(contextWithSakstema("DAG")) shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("FOR"))
            .skalVises(contextWithSakstema()) shouldBe false
        IncludeIfSakstemaContentRule(includeList = listOf("SAF", "SYM"))
            .skalVises(contextWithSakstema("SAF")) shouldBe true
        IncludeIfSakstemaContentRule(includeList = listOf("DAG"))
            .skalVises(contextWithSakstema("SAF", "SYM", "DAG")) shouldBe true
    }
}
