package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.storage.Storage
import io.github.oshai.kotlinlogging.KotlinLogging

class ManifestsStorage(private val storage: Storage, private val bucketName: String) {
    val log = KotlinLogging.logger {  }
    fun getManifestBucketContent(): Map<String, String> = try {
        (storage.readAllBytes(bucketName, manifestFileName).let {
            objectMapper.readValue(String(it), Map::class.java)
        } as Map<String, String>)
    } catch (e: Exception) {
        log.error { """
            Feil i henting av manifestfil:
            ${e.message}
        """.trimIndent() }
        emptyMap()
    }


    companion object {
        const val manifestFileName = "manifests.json"
        private val objectMapper = jacksonObjectMapper()
    }
}