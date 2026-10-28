import com.expediagroup.graphql.plugin.gradle.config.TimeoutConfiguration
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLDownloadSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version(managed.versions.kotlin)
    alias(managed.plugins.tms.jarBundling)
    alias(libs.plugins.graphql)

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
    implementation(libs.caffeine)
    implementation(managed.bundles.database)
    implementation(managed.kotliQuery)
    implementation(managed.kotlinLogging)
    implementation(managed.bundles.ktorServer)
    implementation(managed.ktor.serialization.jackson)
    implementation(managed.ktor.client.contentNegotiation)
    implementation(managed.logback.encoder)
    implementation(managed.tms.ktorTokenSupport.userToken.verification)
    implementation(managed.tms.ktorTokenSupport.userToken.exchange)
    implementation(managed.tms.commonLib.observability)
    implementation(managed.tms.commonLib.utils)
    implementation(managed.tms.commonLib.metrics)
    implementation(managed.tms.commonLib.teamLogger)
    implementation(managed.kotlinx.coroutines)
    implementation(managed.kotlin.reflect)
    implementation(managed.bundles.jackson)
    implementation(managed.jackson.yaml)
    implementation(managed.tms.kafka.application)
    implementation(managed.prometheus.metric.core)
    implementation(libs.jsonpathKt)
    implementation(libs.graphql.kotlinClient)
    implementation(libs.graphql.ktorKotlinClient)
    implementation(libs.google.cloud.storage)

    testImplementation(managed.bundles.junit)
    testImplementation(managed.junit.params)
    testImplementation(managed.bundles.testContainers)
    testImplementation(managed.bundles.kotest)
    testImplementation(managed.ktor.test.server)
    testImplementation(managed.ktor.test.clientMock)
    testImplementation(managed.tms.ktorTokenSupport.userToken.verificationMock)
    testImplementation(managed.mockk)

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

    outputFile.set(layout.buildDirectory.file("graphql/saf.graphql"))
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
    outputFile.set(layout.buildDirectory.file("graphql/pdl.graphql"))
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
