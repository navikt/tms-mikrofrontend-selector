package no.nav.tms.mikrofrontend.selector

import com.fasterxml.jackson.databind.DeserializationFeature
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.ktor.client.*
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
    val rapidTopic: String = getEnvVar("RAPID_TOPIC"),
    val storageBucketName: String = getEnvVar("STORAGE_BUCKET_NAME"),
    val safUrl: String = getEnvVar("SAF_URL"),
    val safClientId: String = getEnvVar("SAF_CLIENT_ID"),
    val aiaUrl: String = getEnvVar("AIA_API_URL"),
    val aiaClientId: String = getEnvVar("AIA_CLIENT_ID"),
    val oppfolgingUrl: String = getEnvVar("OPPFOLGING_API_URL"),
    val oppfolgingClienId: String = getEnvVar("OPPFOLGING_CLIENT_ID"),
    val meldekortUrl:String = getEnvVar("MELDEKORT_BASE_URL"),
    val meldekortClientId :String = getEnvVar("MELDEKORT_CLIENT_ID")

) {


    fun rapidConfig(): Map<String, String> = mapOf(
        "KAFKA_BROKERS" to aivenBrokers,
        "KAFKA_CONSUMER_GROUP_ID" to groupId,
        "KAFKA_RAPID_TOPIC" to rapidTopic,
        "KAFKA_KEYSTORE_PATH" to securityVars.aivenKeystorePath,
        "KAFKA_CREDSTORE_PASSWORD" to securityVars.aivenCredstorePassword,
        "KAFKA_TRUSTSTORE_PATH" to securityVars.aivenTruststorePath,
        "KAFKA_RESET_POLICY" to "earliest",
        "HTTP_PORT" to "8080",
        "NAIS_NAMESPACE" to namespace,
        "NAIS_CLUSTER_NAME" to clusterName
    )

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

fun HttpClientConfig<*>.configureJackson() {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

