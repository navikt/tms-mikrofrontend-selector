import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import no.nav.tms.mikrofrontend.selector.database.Database
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import org.flywaydb.core.Flyway
import org.postgresql.util.PGobject
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

    fun getChangelog(ident: String) = list {
        queryOf("SELECT * FROM changelog where ident=:ident", mapOf("ident" to ident))
            .map {
                ChangelogEntry(
                    originalData = it.stringOrNull("original_data"),
                    newData = it.string("new_data"),
                    date = it.localDateTime("timestamp")
                )
            }.asList
    }

    fun getMicrofrontends(ident: String) = query {
        queryOf("select microfrontends from person where ident=:ident", mapOf("ident" to ident)).map { row ->
            row.string("microfrontends")
        }.asSingle
    }?.let {
        objectMapper.readTree(it)["microfrontends"]
            .toList()
    }

    fun insertWithLegacyFormat(ident: String, vararg microfrontends: String) = update {
        queryOf(
            """INSERT INTO person (ident, microfrontends,created) VALUES (:ident, :newData, :now) 
                    |ON CONFLICT(ident) DO UPDATE SET microfrontends=:newData, last_changed=:now
                """.trimMargin(),
            mapOf(
                "ident" to ident,
                "newData" to microfrontends.toJson(),
                "now" to PersonRepository.LocalDateTimeHelper.nowAtUtc()
            )
        )
    }

    private fun Array<out String>.toJson() = """
        {
        "microfrontends": ${joinToString(separator = ",", prefix = "[", postfix = "]", transform = { """"$it"""" })}
        }
    """.trimIndent().let {
        PGobject().apply {
            type = "jsonb"
            value = it
        }
    }


}


data class ChangelogEntry(val originalData: String?, val newData: String, val date: LocalDateTime)

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
