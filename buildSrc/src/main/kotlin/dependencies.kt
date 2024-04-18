object Flyway9: default.FlywayDefaults {
    override val version = "9.18.0"
}

object GoogleCloud: default.DependencyGroup {
    override val groupId = "com.google.cloud"
    override val version = "2.26.0"

    val storage = dependency("google-cloud-storage")
}

object JacksonExt : default.JacksonDatatypeDefaults {
    val core = dependency("jackson-core", groupId = "com.fasterxml.jackson.core")
    val databind = dependency("jackson-databind", groupId = "com.fasterxml.jackson.core")
    val dataformatYaml = dependency("jackson-dataformat-yaml", groupId = "com.fasterxml.jackson.dataformat")

}
