rootProject.name = "tms-mikrofrontend-selector"
include("app","lib")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven(url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release"))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("managed") {
            from("no.nav.tms:tms-dependency-admin:20261027.8.1")
            library("kotlin-reflect","org.jetbrains.kotlin","kotlin-reflect").versionRef("kotlin")
            library("jackson-yaml","com.fasterxml.jackson.dataformat","jackson-dataformat-yaml").versionRef("jacksonDatatype")
            library("jackson-core","com.fasterxml.jackson.core","jackson-core").versionRef("jacksonDatatype")
            library("jackson-databind","com.fasterxml.jackson.core","jackson-databind").versionRef("jacksonDatatype")
        }
    }
}
