import default.DependencyGroup
import default.TmsCommonLibDefaults
import default.TmsKafkaToolsDefaults

object Caffeine : DependencyGroup {
    override val version = "3.2.3"
    override val groupId = "com.github.ben-manes.caffeine"

    val caffeine = dependency("caffeine")
}

object Flyway9: default.FlywayDefaults {
    override val version = "9.18.0"
}

object GoogleCloud: DependencyGroup {
    override val groupId = "com.google.cloud"
    override val version = "2.26.0"

    val storage = dependency("google-cloud-storage")
}

object GraphQL: DependencyGroup {
    override val groupId get() = "com.expediagroup"
    override val version = "9.2.0"

    val pluginId get() = "com.expediagroup.graphql"

    val kotlinClient get() = dependency("graphql-kotlin-client")
    val kotlinKtorClient get() = dependency("graphql-kotlin-ktor-client")
}

object JacksonExt : default.JacksonDatatypeDefaults {
    val core = dependency("jackson-core", groupId = "com.fasterxml.jackson.core")
    val databind = dependency("jackson-databind", groupId = "com.fasterxml.jackson.core")
    val dataformatYaml = dependency("jackson-dataformat-yaml", groupId = "com.fasterxml.jackson.dataformat")

}

object JacksonJsonPath: DependencyGroup {
    override val groupId: String
        get() = "com.nfeld.jsonpathkt"
    override val version: String
        get() = "2.0.1"

    val core = dependency("jsonpathkt")
}
