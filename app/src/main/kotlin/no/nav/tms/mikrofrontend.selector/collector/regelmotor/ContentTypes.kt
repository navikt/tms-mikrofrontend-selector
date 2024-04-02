package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition


class RegelstyrtMicrofrontend(
    id: String,
    manifestMap: Map<String, String>,
    var contentRules: MutableList<ContentRule> = mutableListOf()
) {
    private val log = KotlinLogging.logger { }
    val definition = MicrofrontendsDefinition.create(id, manifestMap).also {
        if (it == null)
            log.info { "Fant ikke manifest for regelstyre microfrontend med id $id i $manifestMap" }
    }

    fun skalVises() = contentRules.all { it.skalVises() }
}

class Produktkort(
    val id: String,
    var rules: MutableList<ContentRule> = mutableListOf()
) {
    fun skalVises() =
        rules.all { it.skalVises() }
}




