package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.mikrofrontend.selector.collector.MicrofrontendsDefinition
import no.nav.tms.mikrofrontend.selector.versions.DiscoveryManifest


class RegelstyrtMicrofrontend private constructor(
    val definition: MicrofrontendsDefinition?,
    private val rulesDefinition: ContentRulesDefinition
) {
    fun skalVises(context: RuleContext) = rulesDefinition.skalVises(context)

    companion object {
        private val log = KotlinLogging.logger { }

        fun create(rulesDefinition: ContentRulesDefinition, discoveryManifest: DiscoveryManifest): RegelstyrtMicrofrontend {
            val definition = MicrofrontendsDefinition.create(rulesDefinition.id, discoveryManifest).also {
                if (it == null)
                    log.info { "Fant ikke manifest for regelstyre microfrontend med id ${rulesDefinition.id} i $discoveryManifest" }
            }
            return RegelstyrtMicrofrontend(definition, rulesDefinition)
        }
    }
}

class Produktkort(
    val id: String,
    private val rulesDefinition: ContentRulesDefinition
) {
    fun skalVises(context: RuleContext) = rulesDefinition.skalVises(context)
}
