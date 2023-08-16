package no.nav.tms.mikrofrontend.selector.versions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.storage.Storage

class ManifestsStorage(private val storage: Storage, private val bucketName: String) {
    fun getManifestBucketContent(): Map<String, String> =
        (storage.readAllBytes(bucketName, manifestFileName)?.let {
            objectMapper.readValue(String(it), Map::class.java)
        } ?: throw Exception("$manifestFileName er null")) as Map<String, String>


    companion object {
        const val manifestFileName = "manifests.json"
        private val objectMapper = jacksonObjectMapper()
    }
}