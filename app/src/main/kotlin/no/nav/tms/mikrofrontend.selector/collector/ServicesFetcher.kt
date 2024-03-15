package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.tms.mikrofrontend.selector.collector.SafResponse.Companion.toSafKoder
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser

class ServicesFetcher(
    val safUrl: String,
    val safClientId: String,
    val httpClient: HttpClient,
    val tokendingsService: TokendingsService,
    val oppfølgingBase: String,
    val oppfølgingClientId: String,
    val aiaBackendUrl: String,
    val aiaBackendClientId: String,
    val meldekortUrl: String,
    val meldekortClientId: String,
) {

    val log = KotlinLogging.logger { }
    fun query(ident: String) = """ {
        "query": "query(${'$'}ident: String!) {
            dokumentoversiktSelvbetjening(ident:${'$'}ident, tema:[]) {
                tema {
                    kode
                    journalposter{
                        relevanteDatoer {
                            dato
                        }
                    }
                }
              }
           }",
          "variables": {"ident" : "$ident"}
        }
    """.compactJson()

    private fun String.compactJson(): String =
        trimIndent()
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")


    suspend fun fetchSakstema(user: TokenXUser): SafResponse = withErrorHandling {
        httpClient.post {
            url("$safUrl/graphql")
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, safClientId)}")
            header("Content-Type", "application/json")
            setBody(query(user.ident))
        }
            .let { response ->
                if (response.status != HttpStatusCode.OK) {
                    SafResponse(response)
                } else {
                    val jsonResponse = response.bodyAsJsonNode()
                    SafResponse(
                        sakstemakoder = jsonResponse?.getFromPath<List<String>>("data.dokumentoversiktSelvbetjening.tema..kode")
                            ?: emptyList(),
                        errors = jsonResponse?.getFromPath<List<String>>("errors..message") ?: emptyList()
                    )
                }
            }
    }

    suspend fun fetchOppfolging(user: TokenXUser): OppfolgingResponse = withErrorHandling {
        httpClient.get("$oppfølgingBase/api/niva3/underoppfolging") {
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, oppfølgingClientId)}")
            header("Content-Type", "application/json")
            //BODY?
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                OppfolgingResponse(response)
            else
                OppfolgingResponse(underOppfolging = response.bodyAsJsonNode()?.getFromKey<Boolean>("underOppfolging") ?: false)
        }
    }


    suspend fun fetchArbeidsøker(user: TokenXUser): ArbeidsøkerResponse = withErrorHandling {
        httpClient.get("$aiaBackendUrl/aia-backend/er-arbeidssoker") {
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, aiaBackendClientId)}")
            header("Content-Type", "application/json")
            //TODO: body
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                ArbeidsøkerResponse("responsstatus ${response.status}")
            else
                ArbeidsøkerResponse(response.bodyAsJsonNode())
        }
    }

    suspend fun fetchMeldekort(user: TokenXUser): MeldekortResponse = withErrorHandling {
        httpClient.get("$meldekortUrl/api/person/meldekortstatus") {
            header("Authorization", "Bearer ${tokendingsService.exchangeToken(user.tokenString, meldekortClientId)}")
            header("Content-Type", "application/json")
            //TODO: body
        }.let { response ->
            if (response.status != HttpStatusCode.OK)
                MeldekortResponse("responsstatus ${response.status}")
            else
                MeldekortResponse(response.bodyAsJsonNode())
        }
    }

    private suspend fun HttpResponse.bodyAsJsonNode() = NullSafeJson.initObjectMapper(bodyAsText())

    private suspend fun <T> withErrorHandling(function: suspend () -> T) =
        try {
            function()
        } catch (e: Exception) {
            throw ApiException(e)
        }

    class ApiException(e: Exception) :
        Exception("Kall til eksterne tjenester feiler: ${e.message ?: e::class.simpleName}")
}


abstract class ResponseWithErrors(val errors: List<String>) {

    constructor(response: HttpResponse): this(listOf("Status fra ${response.request.url} er ${response.status}"))

    abstract val source: String
    fun errorMessage() = errors.let {
        if (it.isEmpty())
            null
        else
            "Kall til $source feiler: ${errors.joinToString { err -> err }}"
    }
}

class SafResponse : ResponseWithErrors {
    var sakstemakoder: List<String>

    constructor(sakstemakoder: List<String>, errors: List<String>) : super(errors) {
        this.sakstemakoder = sakstemakoder
    }

    constructor(response: HttpResponse) : super(response) {

        this.sakstemakoder = emptyList()
    }

    override val source: String = "SAF"

    companion object {
        fun JsonNode.toSafKoder() =
            toList()
                .map { node -> node["kode"].asText() }
    }
}

class OppfolgingResponse : ResponseWithErrors {
    var underOppfolging: Boolean

    constructor(underOppfolging: Boolean) : super(emptyList()) {
        this.underOppfolging = underOppfolging
    }

    constructor(response: HttpResponse) : super(response) {
        this.underOppfolging = false
    }

    override val source = "Oppfølgingapi"
}

class MeldekortResponse(val todo: Boolean, errors: List<String>) : ResponseWithErrors(errors) {
    //TODO
    override val source = "meldekort"
    fun harMeldekort(): Boolean = false

    constructor(error: String) : this(false, listOf(error))
    constructor(jsonNode: NullSafeJson?) : this(
        false,
        emptyList()
    )

}

class ArbeidsøkerResponse(
    val erArbeidssoker: Boolean,
    val erStandard: Boolean,
    errors: List<String>
) : ResponseWithErrors(errors) {
    override val source = "aia-backend"

    constructor(error: String) : this(erArbeidssoker = false, erStandard = false, listOf(error))
    constructor(jsonNode: NullSafeJson?) : this(
        erArbeidssoker = jsonNode?.getFromKey<Boolean>("erArbeidssoker") ?: false,
        erStandard = jsonNode?.getFromKey<Boolean>("erStandard")?:false ,
        emptyList()
    )
}