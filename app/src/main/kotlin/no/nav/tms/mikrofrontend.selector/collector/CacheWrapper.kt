package no.nav.tms.mikrofrontend.selector.collector

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.time.Duration

class CacheWrapper<V>(
    cacheSize: Long,
    expiryDuration: Duration
) {
    // Bruker async cache med await() for å beholde kompatibilitet med coroutines
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(expiryDuration)
        .buildAsync<String, V>()

    suspend fun get(key: String, supplier: suspend () -> V): V = coroutineScope {
        val cacheGet = cache.get(key) { _, _ ->
            future {
                supplier()
            }
        }

        cacheGet.await()
    }
}
