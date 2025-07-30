package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.databind.DeserializationFeature
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.jackson.*
import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val groupId: String = getEnvVar("GROUP_ID"),
    val dbHost: String = getEnvVar("DB_HOST"),
    val dbPort: String = getEnvVar("DB_PORT"),
    val dbName: String = getEnvVar("DB_DATABASE"),
    val dbUser: String = getEnvVar("DB_USERNAME"),
    val dbPassword: String = getEnvVar("DB_PASSWORD"),
    val dbUrl: String = getDbUrl(dbHost, dbPort, dbName),
    val clusterName: String = getEnvVar("NAIS_CLUSTER_NAME"),
    val namespace: String = getEnvVar("NAIS_NAMESPACE"),
    val aivenBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val aivenSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val securityVars: SecurityVars = SecurityVars(),
    val microfrontendtopic: String = getEnvVar("KAFKA_TOPIC"),
    val storageBucketName: String = getEnvVar("STORAGE_BUCKET_NAME"),
    val safUrl: String = getEnvVar("SAF_URL"),
    val safClientId: String = getEnvVar("SAF_CLIENT_ID"),
    val meldekortApiUrl: String = getEnvVar("MELDEKORT_API_URL"),
    val meldekortApiClientId: String = getEnvVar("MELDEKORT_API_CLIENT_ID"),
    val dpMeldekortUrl: String = getEnvVar("DP_MELDEKORT_URL"),
    val dpMeldekortApiClientId: String = getEnvVar("DP_MELDEKORT_CLIENT_ID"),
    val pdlClientId: String = getEnvVar("PDL_API_CLIENT_ID"),
    val pdlApiUrl: String = getEnvVar("PDL_API_URL"),
    val digisosClientId: String = getEnvVar("DIGISOS_CLIENT_ID"),
    val digisosUrl: String = getEnvVar("DIGISOS_API_URL"),
    val pdlBehandlingsnummer: String = getEnvVar("PDL_BEHANDLINGSNUMMER"),
    val defaultInnsynLenke: String = getEnvVar("DEFAULT_INNSYN_LENKE"),
    val innsynsLenker: Map<String, String> = mapOf(
        "KOM" to getEnvVar("SOSIALHJELP_INNSYN")
    ),
) {
    fun initGcpStorage(): Storage = StorageOptions
        .newBuilder()
        .setProjectId(getEnvVar("GCP_TEAM_PROJECT_ID"))
        .build()
        .service
}

data class SecurityVars(
    val aivenTruststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    val aivenKeystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
    val aivenCredstorePassword: String = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
    val aivenSchemaRegistryUser: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
    val aivenSchemaRegistryPassword: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD")
)

fun getDbUrl(host: String, port: String, name: String): String {
    return if (host.endsWith(":$port")) {
        "jdbc:postgresql://${host}/$name"
    } else {
        "jdbc:postgresql://${host}:${port}/${name}"
    }
}

fun HttpClientConfig<*>.configureClient() {
    install(ClientContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 3000
    }

}

