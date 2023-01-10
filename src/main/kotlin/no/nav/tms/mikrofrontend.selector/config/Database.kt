package no.nav.tms.mikrofrontend.selector.config

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Query
import kotliquery.action.ListResultQueryAction

import kotliquery.sessionOf
import kotliquery.using

interface Database {
    val dataSource: HikariDataSource
    fun update(queryBuilder: () -> Query) {
        using(sessionOf(dataSource)) {
            it.run(queryBuilder.invoke().asUpdate)
        }
    }

    fun <T> query(action: () -> ListResultQueryAction<T>): List<T> =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }

    fun <T> list(action: () -> ListResultQueryAction<T>): List<T> =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }

}
