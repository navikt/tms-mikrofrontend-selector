import com.expediagroup.graphql.plugin.gradle.config.TimeoutConfiguration
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLDownloadSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version(Kotlin.version)

    id(GraphQL.pluginId) version GraphQL.version

    id(TmsJarBundling.plugin)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {

    implementation(TmsCommonLib.utils)
    implementation(Flyway9.core)
    implementation(Hikari.cp)
    implementation(GraphQL.kotlinClient)
    implementation(GraphQL.kotlinKtorClient)
    implementation(KotlinLogging.logging)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.contentNegotiation)
    implementation(Ktor.Client.contentNegotiation)
    implementation(Ktor.Server.auth)
    implementation(Ktor.Server.authJwt)
    implementation(Ktor.Server.statusPages)
    implementation(Ktor.Serialization.jackson)
    implementation(Logstash.logbackEncoder)
    implementation(TmsKtorTokenSupport.userTokenVerification)
    implementation(TmsKtorTokenSupport.userTokenExchange)
    implementation(Postgresql.postgresql)
    implementation(KotliQuery.kotliquery)
    implementation(KotlinLogging.logging)
    implementation(TmsCommonLib.metrics)
    implementation(TmsCommonLib.observability)
    implementation(TmsCommonLib.teamLogger)
    implementation(GoogleCloud.storage)
    implementation(Kotlinx.coroutines)
    implementation(Kotlin.reflect)
    implementation(JacksonExt.dataformatYaml)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(TmsKafkaTools.kafkaApplication)
    implementation(Prometheus.metricsCore)
    implementation(JacksonJsonPath.core)

    testImplementation(JunitPlatform.launcher)
    testImplementation(JunitJupiter.api)
    testImplementation(JunitJupiter.engine)
    testImplementation(JunitJupiter.params)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Ktor.Test.serverTestHost)
    testImplementation(TmsKtorTokenSupport.userTokenVerificationMock)
    testImplementation(Mockk.mockk)
    testImplementation(Ktor.Test.clientMock)

    testImplementation(project(":lib"))

}

application {
    mainClass.set("no.nav.tms.mikrofrontend.selector.ApplicationKt")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}


// Generates graphql schemas for SAF
val graphqlDownloadSafSdl by tasks.register("graphqlDownloadSafSdl", GraphQLDownloadSDLTask::class) {

    outputFile.set(file("${getLayout().buildDirectory}/graphql/saf.graphql"))
    endpoint.set("https://navikt.github.io/safselvbetjening/schema.graphqls")
    timeoutConfig.set(TimeoutConfiguration(connect = 10_000, read = 30_000))
}

val generateSafClient by tasks.register("generateSafClient", GraphQLGenerateClientTask::class) {

    packageName.set("no.nav.dokument.saf.selvbetjening.generated.dto")
    schemaFile.set(graphqlDownloadSafSdl.outputFile)
    // optional
    allowDeprecatedFields.set(true)
    queryFileDirectory.set(File("${project.projectDir.absolutePath}/src/main/resources/graphql/saf"))

    dependsOn("graphqlDownloadSafSdl")
}

// Generates graphql schemas for PDL
val graphqlDownloadPdlSdl by tasks.register("graphqlDownloadPdlSdl", GraphQLDownloadSDLTask::class) {
    outputFile.set(file("${getLayout().buildDirectory}/graphql/pdl.graphql"))
    endpoint.set("https://navikt.github.io/pdl/pdl-api-sdl.graphqls")
    timeoutConfig.set(TimeoutConfiguration(connect = 10_000, read = 30_000))
}

val generatePdlClient by tasks.register("generatePdlClient", GraphQLGenerateClientTask::class) {

    packageName.set("no.nav.pdl.generated.dto")
    schemaFile.set(graphqlDownloadPdlSdl.outputFile)
    // optional
    allowDeprecatedFields.set(true)
    queryFileDirectory.set(File("${project.projectDir.absolutePath}/src/main/resources/graphql/pdl"))

    dependsOn("graphqlDownloadPdlSdl")
}

// Ensures graphql plugins are run as part of compile
tasks.withType<KotlinCompile> {
    dependsOn(generateSafClient, generatePdlClient)
}
