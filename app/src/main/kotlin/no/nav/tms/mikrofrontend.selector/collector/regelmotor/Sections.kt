package no.nav.tms.mikrofrontend.selector.collector.regelmotor

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathkt.extension.read
import no.nav.tms.mikrofrontend.selector.database.Microfrontends

abstract class Section(val name: String, private val microfrontendIds: List<String>) {
    fun getMicrofrontendsForSection(enabledMicrofrontends: Microfrontends?, innloggetNivå:Int) = enabledMicrofrontends?.ids(innloggetNivå)
        ?.let {enabledIds -> microfrontendIds.intersect(enabledIds.toSet()) }
        ?: emptyList()

    companion object {
        fun JsonNode.getSection(name: String) = this.read<List<String>>("\$sections.$name")?.let {
            object : Section(name, it) {}
        } ?: throw IllegalArgumentException("Section with name $name is not defined")
    }
}



