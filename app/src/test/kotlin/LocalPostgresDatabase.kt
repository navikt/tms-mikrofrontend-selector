import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.mikrofrontend.selector.database.Database
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import org.flywaydb.core.Flyway
import org.postgresql.util.PGobject
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime

class LocalPostgresDatabase private constructor() : Database {

    private val memDataSource: HikariDataSource
    private val container = PostgreSQLContainer<Nothing>("postgres:14.5")

    companion object {
        private val instance by lazy {
            LocalPostgresDatabase().also {
                it.migrate(2)
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

    private fun migrate(expectedMigrations: Int) {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(dataSource)
            .load()
            .migrate()
            .also {
                it.migrationsExecuted shouldBe expectedMigrations
            }
    }

    fun getChangelog(ident: String) = list {
        queryOf("SELECT * FROM changelog where ident=:ident", mapOf("ident" to ident))
            .map {
                ChangelogEntry(
                    originalData = it.stringOrNull("original_data"),
                    newData = it.string("new_data"),
                    date = it.localDateTime("timestamp"),
                    initiatedBy = it.stringOrNull("initiated_by")
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

    fun insertLegacyFormat(
        ident: String,
        format: (String) -> String,
        vararg microfrontends: String,
    ) = update {
        queryOf(
            """INSERT INTO person (ident, microfrontends,created) VALUES (:ident, :newData, :now) 
                    |ON CONFLICT(ident) DO UPDATE SET microfrontends=:newData, last_changed=:now
                """.trimMargin(),
            mapOf(
                "ident" to ident,
                "newData" to microfrontends.legacyDbObject(format),
                "now" to PersonRepository.LocalDateTimeHelper.nowAtUtc()
            )
        )
    }
}

fun dbV1(vararg microfrontends: String) = """
        {
        "microfrontends": ${
    microfrontends.joinToString(
        separator = ",",
        prefix = "[",
        postfix = "]",
        transform = { """"$it"""" })
}
        }
    """.trimIndent().let {
    PGobject().apply {
        type = "jsonb"
        value = it
    }
}

private fun Array<out String>.legacyDbObject(transform: (String) -> String) = """
        {
        "microfrontends": ${
    joinToString(
        separator = ",",
        prefix = "[",
        postfix = "]",
        transform = transform
    )
}
        }
    """.trimIndent().let {
    PGobject().apply {
        type = "jsonb"
        value = it
    }
}

fun dbv1Format(value: String) = """"$value""""
fun dbv2Format(value: String) = """ {"microfrontend_id":"$value", "sikkerhetsnivå":4}""".trimIndent()

fun dbV2(vararg microfrontends: String) = """
        {
            "microfrontends": ${
    microfrontends.joinToString(
        separator = ",",
        prefix = "[",
        postfix = "]"
    )
    { """ {"microfrontend_id":"$it", "sikkerhetsnivå":4}""".trimIndent() }
}
        }""".trimIndent().let {
    PGobject().apply {
        type = "jsonb"
        value = it
    }
}

data class ChangelogEntry(
    val originalData: String?,
    val newData: String,
    val date: LocalDateTime,
    val initiatedBy: String?
)

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
