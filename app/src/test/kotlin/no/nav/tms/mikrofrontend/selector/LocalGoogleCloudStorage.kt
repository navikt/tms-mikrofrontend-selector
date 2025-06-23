package no.nav.tms.mikrofrontend.selector

import com.google.cloud.NoCredentials
import com.google.cloud.storage.*
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage.Companion.manifestFileName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets

class GoogleCloudStorageTestContainer : GenericContainer<GoogleCloudStorageTestContainer>(
    DockerImageName.parse("fsouza/fake-gcs-server")
) {
    lateinit var gcpHostUrl: String

    init {
        withExposedPorts(4443)
        withCreateContainerCmdModifier {
            it.withEntrypoint("/bin/fake-gcs-server", "-scheme", "http")
        }
    }

    override fun start() {
        super.start()
        gcpHostUrl = "http://${this.host}:${this.firstMappedPort}"

        updateExternalUrlWithContainerUrl(gcpHostUrl)
    }

    private fun updateExternalUrlWithContainerUrl(fakeGcsExternalUrl: String) {
        val modifyExternalUrlRequestUri = "$fakeGcsExternalUrl/_internal/config"
        val updateExternalUrlJson = ("{"
                + "\"externalUrl\": \"" + fakeGcsExternalUrl + "\""
                + "}")
        val req: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(modifyExternalUrlRequestUri))
            .header("Content-Type", "application/json")
            .PUT(BodyPublishers.ofString(updateExternalUrlJson))
            .build()
        val response: HttpResponse<Void> = HttpClient.newBuilder().build()
            .send(req, BodyHandlers.discarding())
        if (response.statusCode() != 200) {
            throw RuntimeException(
                "error updating fake-gcs-server with external url, response status code " + response.statusCode() + " != 200"
            )
        }
    }

}

class LocalGCPStorage {

    fun updateManifest(expectedMicrofrontends: MutableMap<String, String>) {
        val toStorage = expectedMicrofrontends.toMutableMap()
        toStorage.putAll(akuteltMicrofrontends)
        val contents = "{ ${toStorage.map { """"${it.key}": { "url":"${it.value}", "appname": "name", "namespace": "space", "fallback": "http://name/fallback", "ssr": true }""" }
            .joinToString(separator = ",")} }"
        val blobid = BlobId.of(testBucketName, manifestFileName)
        val blobInfo = BlobInfo.newBuilder(blobid).build()
        val content: ByteArray = contents.toByteArray(StandardCharsets.UTF_8)
        storage.createFrom(blobInfo, ByteArrayInputStream(content), Storage.BlobWriteOption.detectContentType())
    }

    private val container = GoogleCloudStorageTestContainer().also { it.start() }

    val storage: Storage = StorageOptions
        .newBuilder()
        .setProjectId(testProjectId)
        .setHost(container.gcpHostUrl)
        .setCredentials(NoCredentials.getInstance())
        .build()
        .service

    init {
        storage.create(BucketInfo.of(testBucketName))
    }


    companion object {
        val akuteltMicrofrontends = mapOf("pensjonskalkulator-microfrontend" to "https://cdn.pensjon/manifest.json")

        const val testBucketName = "test-bucket"
        const val testProjectId = "test-project"
        val instance by lazy {
            LocalGCPStorage()
        }
    }

}



