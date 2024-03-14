package no.nav.tms.mikrofrontend.selector.collector

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.tms.mikrofrontend.selector.collector.Produktkort.Companion.ids
import no.nav.tms.mikrofrontend.selector.database.PersonRepository
import no.nav.tms.mikrofrontend.selector.metrics.ProduktkortCounter
import no.nav.tms.mikrofrontend.selector.versions.ManifestsStorage
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser
import java.time.Instant
import kotlin.random.Random

class PersonalContentCollector(
    val repository: PersonRepository,
    val manifestStorage: ManifestsStorage,
    val sakstemaFetcher: SakstemaFetcher,
    val produktkortCounter: ProduktkortCounter
) {
    suspend fun getContent(user: TokenXUser, innloggetnivå: Int): PersonalContentResponse {
        val microfrontends = repository.getEnabledMicrofrontends(user.ident)
        val safResponse = sakstemaFetcher.fetchSakstema(user)
        return PersonalContentResponse(
            microfrontends = microfrontends?.getDefinitions(innloggetnivå, manifestStorage.getManifestBucketContent())
                ?: emptyList(),
            produktkort = ProduktkortVerdier
                .resolveProduktkort(
                    koder = safResponse.sakstemakoder,
                    ident = user.ident,
                    microfrontends = microfrontends
                ).ids().also { produktkortCounter.countProduktkort(it) },
            offerStepup = microfrontends?.offerStepup(innloggetnivå = innloggetnivå) ?: false
        ).apply {
            if (safResponse.hasErrors)
                safError = safResponse.errors.joinToString { it }
        }
    }

    /*
    suspend fun asyncf(user: TokenXUser): Pair<SafResponse, String> {
        return coroutineScope {
            val safResponse = async { sakstemaFetcher.fetchSakstema(user) }
            val sleepResponse = async { sakstemaFetcher.sleepTest() }
            return@coroutineScope Pair(safResponse.await(), sleepResponse.await())
        }
    }*/


    class PersonalContentResponse(
        val microfrontends: List<MicrofrontendsDefinition>,
        val produktkort: List<String>,
        val offerStepup: Boolean
    ) {
        @JsonIgnore
        var safError: String? = null
        fun resolveStatus(): HttpStatusCode = if (safError != null) HttpStatusCode.MultiStatus else HttpStatusCode.OK
    }

    class MicrofrontendsDefinition(
        @JsonProperty("microfrontend_id")
        val id: String,
        val url: String
    )
}

suspend fun fetchStockPrice(stock: String): Double {
    println("Fetching stock $stock, ${Instant.now()}")
    delay(Random.nextLong(1000)) // Simulating variable data retrieval delay
    println("Stock $stock collected")
    return Random.nextDouble(50.0, 200.0)
}

fun main() = runBlocking {
    val stocks = listOf("AAPL", "GOOGL", "AMZN", "MSFT")
    val deferredStockPrices = stocks.map { stock ->
        async { stock to fetchStockPrice(stock) }
    }

    val stockPrices = deferredStockPrices.map { deferred ->
        val (stock, price) = deferred.await()
        "$stock: $price"
    }

    stockPrices.forEach(::println)
}


