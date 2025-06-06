package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.versions.MicrofrontendManifest


class RegelstyrtMicrofrontend(
    id: String,
    manifestMap: MicrofrontendManifest,
    var contentResolvers: MutableList<ContentResolver> = mutableListOf()
) {
    private val log = KotlinLogging.logger { }
    val definition = MicrofrontendsDefinition.create(id, manifestMap).also {
        if (it == null)
            log.info { "Fant ikke manifest for regelstyre microfrontend med id $id i $manifestMap" }
    }

    fun skalVises() = contentResolvers.all { it.skalVises() }
}

class Produktkort(
    val id: String,
    var rules: MutableList<ContentResolver> = mutableListOf()
) {
    fun skalVises() =
        rules.all { it.skalVises() }
}




