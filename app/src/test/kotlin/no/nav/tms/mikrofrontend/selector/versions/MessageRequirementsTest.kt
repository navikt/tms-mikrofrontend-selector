package no.nav.tms.mikrofrontend.selector.versions

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.withClue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.tms.common.testutils.assert
import no.nav.tms.mikrofrontend.selector.ident
import no.nav.tms.mikrofrontend.selector.microfrontendId
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.DisableMessage
import no.nav.tms.mikrofrontend.selector.versions.JsonMessageVersions.EnableMessage
import org.junit.jupiter.api.Test


internal class MessageVersionsTest {

    @Test
    fun `setter riktige key requirements for enable`() {
        JsonMessage.newMessage(mapOf("microfrontend_id" to "12358mk", "ident" to "887766")).apply {
            EnableMessage.requireCommonKeys(this)
            EnableMessage.interestedInCurrentVersionKeys(this)
        }.assert {
            shouldNotThrow<IllegalArgumentException> {
                microfrontendId
                ident
            }
            listOf("@initiated_by", "sensitivitet").forEach { key ->
                withClue("interestedIn $key er ikke satt") {
                    shouldNotThrow<IllegalArgumentException> { this[key].asText() }
                }
            }
        }
    }

    @Test
    fun `setter riktige key requirements for disable`() {
        JsonMessage.newMessage(mapOf("microfrontend_id" to "12358mk", "ident" to "887766")).apply {
            DisableMessage.requireCommonKeys(this)
            DisableMessage.interestedInCurrentVersionKeys(this)
        }.assert {
            shouldNotThrow<IllegalArgumentException> {
                microfrontendId
                ident
            }
            listOf("@initiated_by").forEach { key ->
                withClue("interestedIn $key er ikke satt") {
                    shouldNotThrow<IllegalArgumentException> { this[key].asText() }
                }
            }
        }
    }


}