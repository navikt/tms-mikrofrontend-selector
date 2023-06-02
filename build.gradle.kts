plugins {
    kotlin("jvm").version(Kotlin.version)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks {
    jar {
        enabled = false
    }
}
