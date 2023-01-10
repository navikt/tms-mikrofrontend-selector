import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
            instance.update { queryOf("delete from microfrontend") }
            instance.update { queryOf("delete from changelog") }
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
                HistoryContent(
                    oldData = it.string("old_data"),
                    newData = it.string("new_data"),
                    date = it.localDateTime("date")
                )
            }.asList
    }
}

data class HistoryContent(val oldData: String?, val newData: String, val date: LocalDateTime)

private fun Map<String, String>.toJson(): String = mapValues { (_, v) ->
    JsonPrimitive(v)
}.let { JsonObject(it) }
    .toString()

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
