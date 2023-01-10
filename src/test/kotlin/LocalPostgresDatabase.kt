import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import no.nav.tms.mikrofrontend.selector.config.Database
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime

class LocalPostgresDatabase private constructor() : Database {

    private val memDataSource: HikariDataSource
    private val container = PostgreSQLContainer("postgres:14.5")

    companion object {
        private val instance by lazy {
            LocalPostgresDatabase().also {
                it.migrate()
            }
        }

        fun cleanDb(): LocalPostgresDatabase {
            instance.update { queryOf("delete from changelog") }
            instance.update { queryOf("delete from person") }
            return instance
        }
    }

    init {
        container.start()
        memDataSource = createDataSource()
    }

    override val dataSource: HikariDataSource
        get() = memDataSource

    private fun createDataSource(): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            isAutoCommit = true
            validate()
        }
    }

    private fun migrate() {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    fun getChangelog(fnr: String) = list {
        queryOf("SELECT * FROM changelog where ident=:fnr", mapOf("fnr" to fnr))
            .map {
                ChangelogEntry(
                    originalData = it.stringOrNull("original_data"),
                    newData = it.string("new_data"),
                    date = it.localDateTime("timestamp")
                )
            }.asList
    }
}

data class ChangelogEntry(val originalData: String?, val newData: String, val date: LocalDateTime)

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
