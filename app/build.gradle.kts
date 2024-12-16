import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
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
    implementation(TmsKtorTokenSupport.tokenXValidation)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(Postgresql.postgresql)
    implementation(KotliQuery.kotliquery)
    implementation(KotlinLogging.logging)
    implementation(TmsCommonLib.metrics)
    implementation(TmsCommonLib.observability)
    implementation(GoogleCloud.storage)
    implementation(Kotlinx.coroutines)
    implementation(Kotlin.reflect)
    implementation(JacksonExt.dataformatYaml)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(TmsKafkaTools.kafkaApplication)
    implementation(Prometheus.metricsCore)
    implementation(JacksonJsonPath.core)

    testImplementation(Junit.api)
    testImplementation(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Ktor.Test.serverTestHost)
    testImplementation(TmsKtorTokenSupport.tokenXValidationMock)
    testImplementation(Mockk.mockk)
    testImplementation(Ktor.Test.clientMock)
    testImplementation(TmsCommonLib.testutils)

    testImplementation(project(":lib"))

}

application {
    mainClass.set("no.nav.tms.mikrofrontend.selector.ApplicationKt")
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }
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
