package no.nav.tms.mikrofrontend.selector.database

import mu.KotlinLogging


private val log = KotlinLogging.logger { }


object JsonVersions {

    fun latestVersion(id: String, sensitivitet: Sensitivitet) {

    }
}

fun test() {
    Sensitivitet.HIGH > Sensitivitet.SUBSTANTIAL
}

enum class Sensitivitet(val korresponderendeSikkerhetsnivå: Int) {
    HIGH(4), SUBSTANTIAL(3);

    fun innholdKanVises(other: Sensitivitet): Boolean =
        korresponderendeSikkerhetsnivå >= other.korresponderendeSikkerhetsnivå

    fun innholdKanVises(other: Int): Boolean =
        korresponderendeSikkerhetsnivå >= resolve(other).korresponderendeSikkerhetsnivå

    fun innholdKanVises(s: String) = korresponderendeSikkerhetsnivå >= valueOf(s).korresponderendeSikkerhetsnivå


    companion object {
        fun resolve(sikkerhetsnivå: Int?) = when (sikkerhetsnivå) {
            null -> HIGH
            4 -> HIGH
            3 -> SUBSTANTIAL
            else -> {
                log.error { "$sikkerhetsnivå har ingen korresponederende sensitivitetsnviå. Returnerer default-verdi HIGH" }
                HIGH
            }
        }
    }
}



