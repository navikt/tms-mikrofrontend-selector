package no.nav.tms.mikrofrontend.selector

import com.google.api.RoutingProto.routing
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*


const val testHost = "http://test.nav.no"

abstract class RouteProvider(
    val endpoint: String,
    val contentProvider: String,
    val statusCode: HttpStatusCode
) {
    suspend fun PipelineContext<Unit, ApplicationCall>.response() {
        call.respondText(
            contentType = ContentType.Application.Json,
            status = statusCode,
            provider = { contentProvider }
        )
    }

    abstract fun Routing.initRoute()
}


class SafProvider(sakstemaer: List<String> = emptyList(), errorMsg: String? = null) :
    RouteProvider(endpoint = "graphql", contentProvider = sakstemaer.safResponse(errorMsg), statusCode = OK) {

    override fun Routing.initRoute() {
        post(endpoint) {
            response()
        }
    }

    companion object {
        private fun List<String>.safResponse(errorMsg: String?): String {
            val errors = errorMsg?.let {
                """
                  "errors": [
                                  {
                                    "message": "$it",
                                    "locations": [
                                      {
                                        "line": 2,
                                        "column": 3
                                      }
                                    ],
                                    "path": [
                                      "journalpost"
                                    ],
                                    "extensions": {
                                      "code": "not_found",
                                      "classification": "ExecutionAborted"
                                    }
                                  }
                                ],  
                """.trimIndent()
            } ?: ""

            return """
                {
                      $errors  
                      "data": {
                        "dokumentoversiktSelvbetjening": {
                          "tema": ${
                joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { """{ "kode": "$it" }""".trimIndent() }
            }
                          }
                        }
                      }
                    }
                ""${'"'}.trimIndent()
            """.trimIndent()

        }
        val emptySafProvider = SafProvider()
    }
}

class GetProvider(endpoint: String, contentProvider: String, statusCode: HttpStatusCode) :
    RouteProvider(endpoint, contentProvider, statusCode) {
    override fun Routing.initRoute() {
        get(endpoint) {
            response()
        }
    }
}

fun ApplicationTestBuilder.initExternalServices(
    vararg routeProviders: RouteProvider
) = externalServices {
    hosts(testHost) {
        routing {
            routeProviders.forEach { provider ->
                provider.run { this@routing.initRoute() }
            }
        }
    }
}
