object RapidsAndRiversClassCast: default.RapidsAndRiversDefaults {
    override val version = "classcast-feil-SNAPSHOT"
}

object Flyway9: default.FlywayDefaults {
    override val version = "9.18.0"
}

object GoogleCloud: default.DependencyGroup {
    override val groupId = "com.google.cloud"
    override val version = "2.26.0"

    val storage = dependency("google-cloud-storage")
}
