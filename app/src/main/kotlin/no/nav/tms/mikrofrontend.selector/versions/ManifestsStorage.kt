package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.storage.Storage
import io.github.oshai.kotlinlogging.KotlinLogging

class ManifestsStorage(private val storage: Storage, private val bucketName: String) {
    val log = KotlinLogging.logger {  }

    fun getManifestBucketContent(): MicrofrontendManifest {
        try {
            val content = storage.readAllBytes(bucketName, manifestFileName)
            val manifest = objectMapper.readValue(content, MicrofrontendManifest::class.java)

            return manifest
        } catch (e:Exception) {
            log.error { "Feil i henting av manifestfil: ${e.message}" }
            return MicrofrontendManifest(emptyMap())
        }
    }

    companion object {
        const val manifestFileName = "manifests-v2.json"
        private val objectMapper = jacksonObjectMapper()
    }
}

data class MicrofrontendManifest (
    @JsonAnySetter
    val entry: Map<String, Entry>,
)

data class Entry (
    val url: String,
    val appname: String,
    val namespace: String,
    val ssr: Boolean
)