package com.sivan.brickradar.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request

// Väljer mellan Tailscale MagicDNS-adressen och den lokala hemma-IP:n genom att racea en
// lättviktig hälsokontroll (GET .../api/categories) mot båda samtidigt - se issue #19 i
// Sivan87/BrickRadar. Vinnaren cachas för resten av appsessionen (ModelRepository.safeCall
// anropar bara ensureResolved(), som är en no-op efter första lyckade racet) - en plötslig
// IOException i safeCall (t.ex. telefonen bytte nätverk mitt i sessionen) racear om via
// resolve(force = true) istället för att racea om vid varje enskilt API-anrop.
object BackendResolver {
    private val candidates: List<String> = listOf(ApiConfig.TAILSCALE_URL, ApiConfig.LOCAL_URL)

    // Egna, korta timeouts - oberoende av RetrofitClients vanliga klient. En "död" adress
    // (Tailscale avstängt, eller utanför hemma-WiFi) ska inte kunna hänga racet längre än
    // så här innan den andra kandidaten hinner svara.
    private val healthCheckClient = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .build()

    private val mutex = Mutex()

    @Volatile
    private var resolved: String? = null

    // Synkron getter - används av ApiConfig.BASE_URL/RetrofitClients interceptor, som inte
    // kan vänta in en suspend-race. Innan första lyckade resolve() faller den tillbaka på
    // Tailscale-adressen, samma beteende appen redan hade innan denna issue.
    fun currentBaseUrl(): String = resolved ?: candidates.first()

    suspend fun ensureResolved(): String = resolved ?: resolve(force = false)

    suspend fun resolve(force: Boolean): String = mutex.withLock {
        if (!force) {
            resolved?.let { return@withLock it }
        }
        val winner = raceHealthChecks() ?: candidates.first()
        resolved = winner
        winner
    }

    // Returnerar den FÖRSTA kandidaten vars hälsokontroll faktiskt lyckas - inte bara den
    // som svarar snabbast rakt av. Om den snabbaste kandidaten failar (fel nätverk) väntar
    // vi in nästa istället för att ge upp direkt, vilket täcker "båda aktiva samtidigt"-
    // fallet lika väl som "bara den ena är uppe"-fallen.
    private suspend fun raceHealthChecks(): String? = coroutineScope {
        val pending = candidates
            .map { candidate -> async(Dispatchers.IO) { candidate to respondsToHealthCheck(candidate) } }
            .toMutableList()
        var winner: String? = null
        while (pending.isNotEmpty() && winner == null) {
            val (finished, candidate, ok) = select<Triple<Deferred<Pair<String, Boolean>>, String, Boolean>> {
                pending.forEach { deferred ->
                    deferred.onAwait { (candidate, ok) -> Triple(deferred, candidate, ok) }
                }
            }
            pending.remove(finished)
            if (ok) winner = candidate
        }
        pending.forEach { it.cancel() }
        winner
    }

    private fun respondsToHealthCheck(baseUrl: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/categories")
                .header("X-API-Key", ApiConfig.API_KEY)
                .build()
            // Statuskoden spelar ingen roll - även ett 401/404 bevisar att värden faktiskt
            // svarar på HTTP, vilket är allt racet bryr sig om.
            healthCheckClient.newCall(request).execute().use { true }
        } catch (e: IOException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
