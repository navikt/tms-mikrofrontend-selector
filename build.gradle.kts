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
    implementation(DittNAV.Common.influxdb)
    implementation(DittNAV.Common.utils)
    implementation(Flyway.core)
    implementation(Hikari.cp)
    implementation(Influxdb.java)
    implementation(KotlinLogging.logging)
    implementation(Ktor2.Server.core)
    implementation(Ktor2.Server.netty)
    implementation(Ktor2.Server.contentNegotiation)
    implementation(Ktor2.Server.auth)
    implementation(Ktor2.Server.authJwt)
    implementation(Ktor2.jackson)
    implementation(Ktor2.TmsTokenSupport.tokenXValidation)
    implementation(Ktor2.TmsTokenSupport.authenticationInstaller)
    implementation(Postgresql.postgresql)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.logback)
    implementation(RapidsAndRivers)
    implementation(kotliquery)

    testImplementation(Junit.api)
    testImplementation(Junit.engine)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Ktor2.Test.serverTestHost)
    testImplementation(Ktor2.TmsTokenSupport.authenticationInstallerMock)
    testImplementation(Ktor2.TmsTokenSupport.tokenXValidationMock)
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.1.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.20")
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

    register("runServer", JavaExec::class) {
        environment("KAFKA_BROKERS", "localhost:29092")
        environment("KAFKA_SCHEMA_REGISTRY", "http://localhost:8081")
        environment("GROUP_ID", "dittnav_varselbestiller")
        environment("DB_HOST", "localhost")
        environment("DB_PORT", "5432")
        environment("DB_DATABASE", "mikrofrontend_selector")
        environment("DB_USERNAME", "testuser")
        environment("DB_PASSWORD", "testpassword")
        environment("NAIS_CLUSTER_NAME", "dev-gcp")
        environment("NAIS_NAMESPACE", "dev")

        main = application.mainClass.get()
        classpath = sourceSets["main"].runtimeClasspath
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// TODO: Fjern følgende work around i ny versjon av Shadow-pluginet:
// Skal være løst i denne: https://github.com/johnrengelman/shadow/pull/612
project.setProperty("mainClassName", application.mainClass.get())
apply(plugin = Shadow.pluginId)
