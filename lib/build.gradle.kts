import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)
    `java-library`
    `maven-publish`
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
    implementation(JacksonExt.core)
    implementation(JacksonExt.databind)

    testImplementation(Junit.api)
    testImplementation(Junit.engine)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
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

val libraryVersion: String = properties["lib_version"]?.toString() ?: "latest-local"

publishing {
    repositories{
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/tms-mikrofrontend-selector")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            groupId = "no.nav.tms.mikrofrontend.selector"
            artifactId = "builder"
            version = libraryVersion
            from(components["java"])

            val sourcesJar by tasks.creating(Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets.main.get().allSource)
            }

            artifact(sourcesJar)
        }
    }
}
