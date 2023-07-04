import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.allopen").version(Kotlin.version)

    id(Flyway.pluginId) version (Flyway.version)
    id(Shadow.pluginId) version (Shadow.version)

    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven ( "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven" )
    mavenLocal()
}

dependencies {
    implementation(DittNAVCommonLib.utils)
    implementation(Flyway.core)
    implementation(Hikari.cp)
    implementation(Influxdb.java)
    implementation(KotlinLogging.logging)
    implementation(Ktor2.Server.core)
    implementation(Ktor2.Server.netty)
    implementation(Ktor2.Server.contentNegotiation)
    implementation(Ktor2.Server.auth)
    implementation(Ktor2.Server.authJwt)
    implementation(Ktor2.Server.statusPages)
    implementation(Ktor2.Serialization.jackson)
    implementation(TmsKtorTokenSupport.tokenXValidation)
    implementation(TmsKtorTokenSupport.authenticationInstaller)
    implementation(Postgresql.postgresql)
    implementation(RapidsAndRiversClassCast.rapidsAndRivers)
    implementation(KotliQuery.kotliquery)
    implementation(Logback.classic)
    implementation(KotlinLogging.logging)

    testImplementation(Junit.api)
    testImplementation(Junit.engine)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Ktor2.Test.serverTestHost)
    testImplementation(TmsKtorTokenSupport.authenticationInstallerMock)
    testImplementation(TmsKtorTokenSupport.tokenXValidationMock)
    testImplementation(Mockk.mockk)

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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
