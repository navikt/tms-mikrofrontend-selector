package no.nav.tms.mikrofrontend.selector

class DokumentarkivUrlResolver(
    private val temaspesifikkeLenker: Map<String, String>,
    private val generellLenke: String
) {
    fun urlFor(kode: String): String =
        temaspesifikkeLenker.getOrDefault(kode, "$generellLenke/$kode")

}