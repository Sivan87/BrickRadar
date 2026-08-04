package com.sivan.brickradar.repository

import com.sivan.brickradar.model.AddMissingPartRequest
import com.sivan.brickradar.model.AddModelRequest
import com.sivan.brickradar.model.AddSourceRequest
import com.sivan.brickradar.model.AppVersionResponse
import com.sivan.brickradar.model.Brick4SearchResult
import com.sivan.brickradar.model.BuildStatusUpdateRequest
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.CategoryUpdateRequest
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.ModelUpdateRequest
import com.sivan.brickradar.model.MissingPartsResponse
import com.sivan.brickradar.model.OrderNumberUpdateRequest
import com.sivan.brickradar.model.RebrickableSetNumUpdateRequest
import com.sivan.brickradar.model.Receipt
import com.sivan.brickradar.model.ShippingOverrideRequest
import com.sivan.brickradar.model.SourceOverrideRequest
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.StatusUpdateRequest
import com.sivan.brickradar.model.ToggleMissingPartFoundRequest
import com.sivan.brickradar.model.UpdateSourceRequest
import com.sivan.brickradar.network.BackendResolver
import com.sivan.brickradar.network.BrickRadarApi
import com.sivan.brickradar.network.GitHubReleasesApi
import com.sivan.brickradar.network.RetrofitClient
import com.squareup.moshi.Json
import java.io.IOException
import okhttp3.MultipartBody
import retrofit2.HttpException

class ModelRepository(
    private val api: BrickRadarApi = RetrofitClient.api,
    private val githubApi: GitHubReleasesApi = RetrofitClient.githubApi,
) {
    suspend fun getModels(
        status: String? = null,
        category: String? = null,
        valueRating: String? = null,
        sort: String? = null,
    ): ApiResult<List<Model>> = safeCall {
        api.getModels(status, category, valueRating, sort)
    }

    suspend fun getModel(id: Int): ApiResult<Model> = safeCall {
        api.getModel(id)
    }

    suspend fun getCategories(): ApiResult<List<Category>> = safeCall {
        api.getCategories().categories
    }

    suspend fun getStats(): ApiResult<StatsResponse> = safeCall {
        api.getStats()
    }

    // Hämtar direkt från GitHub Releases (publikt repo, issue #1) istället för
    // vår egen /api/app-version — funkar därför oavsett hemma-WiFi eller inte.
    // Fail-silent-kravet (uppdateringskollen ska aldrig störa vanlig
    // appanvändning om GitHub inte är nåbart) hanteras av ANROPAREN
    // (UpdateViewModel ignorerar ApiResult.Error helt) — safeCall/ApiResult
    // är samma väg som resten av repositoryt, ingen särbehandling behövs här.
    // resolveBackend = false: detta anrop rör bara GitHub (githubApi), aldrig vår egen
    // server, så det ska varken vänta in eller trigga BackendResolvers Tailscale/LAN-race
    // (se safeCall nedan och issue #19).
    suspend fun getAppVersion(): ApiResult<AppVersionResponse> = safeCall(resolveBackend = false) {
        val release = githubApi.getLatestRelease()
        val versionName = release.tagName.removePrefix("v")
        // Samma tolkning som backendens tidigare _fetch_latest_github_release/
        // api_app_version: versionCode är det sista heltalet efter versionNamnets
        // sista punkt (t.ex. "1.4" -> 4), matchar "1.$VERSION_CODE"-formatet
        // app/build.gradle.kts/release-workflowet redan producerar.
        val versionCode = versionName.substringAfterLast(".").toIntOrNull() ?: 0
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            ?: throw IOException("Senaste GitHub Release saknar en bifogad APK")
        AppVersionResponse(
            versionCode = versionCode,
            versionName = versionName,
            releaseNotes = release.body.orEmpty(),
            downloadUrl = apkAsset.browserDownloadUrl,
        )
    }

    suspend fun deleteModel(id: Int): ApiResult<Unit> = safeCall {
        api.deleteModel(id).close()
    }

    suspend fun updateStatus(id: Int, status: String): ApiResult<Model> = safeCall {
        api.updateStatus(id, StatusUpdateRequest(status))
    }

    suspend fun updateModel(id: Int, name: String, pieceCount: Int, category: String, notes: String): ApiResult<Model> = safeCall {
        api.updateModel(id, ModelUpdateRequest(name, pieceCount, category, notes))
    }

    // POST /api/models ignorerar helt ett ev. category-fält i body:n — servern
    // sätter alltid category = suggest_category(name) (nyckelordsgissning), se
    // kommentaren i AddModelRequest. category == null här betyder "behåll
    // auto-gissningen" (inget extra anrop); annars görs en uppföljande PUT som
    // BARA sätter category (CategoryUpdateRequest, _apply_model_fields
    // accepterar ett delvis fältset) — inte en fullständig ModelUpdateRequest,
    // eftersom name/pieceCount kan vara null i sökflödet (Fas 7) innan
    // bakgrundshämtningen (initial_fetch) fyllt dem i; att skicka med tomma
    // värden där hade riskerat att skriva över det bakgrundshämtningen redan
    // hunnit sätta.
    suspend fun addModel(
        modelNumber: String,
        brand: String,
        name: String?,
        pieceCount: Int?,
        status: String,
        imageUrl: String?,
        category: String?,
    ): ApiResult<Model> = safeCall {
        val created = api.addModel(AddModelRequest(modelNumber, brand, name, pieceCount, status, imageUrl))
        if (category != null) {
            api.updateCategory(created.id, CategoryUpdateRequest(category))
        } else {
            created
        }
    }

    // GET /api/brick4/search-by-number — returnerar en kandidat per märke med
    // exakt modellnummerträff (se Brick4SearchResult). Tom lista = inga
    // träffar (inte ett fel) — det är ett normalt utfall, inte safeCall-Error.
    suspend fun searchBrick4ByNumber(modelNumber: String): ApiResult<List<Brick4SearchResult>> = safeCall {
        api.searchBrick4ByNumber(modelNumber)
    }

    // Leveranstid (delivery_estimate) hör inte hemma i prices-tabellen (den har
    // bara price/currency/url/in_stock/warehouse) — den lagras i en egen tabell
    // (model_source_overrides, nyckel modell-id+källnamn) och sätts/tas bort via
    // ett helt annat API-anrop (source-override), samma som webb-UI:ts separata
    // "Lagerstatus/lagerland/leveranstid"-dialog använder (se static/app.js).
    // Formuläret i appen visar allt i EN sheet, så varje spara-tryck kan behöva
    // två serveranrop: dels prisraden (append-only, se PUT-kommentaren i
    // BrickRadarApi), dels overriden. Modellen hämtas alltid om sist, dels för att
    // få tillbaka rätt kr/del/best_-beräkningar (som POST/PUT/DELETE .../sources
    // INTE returnerar), dels för att få den nya prisradens NYA id (append-only —
    // den gamla raden lever kvar orörd i historiken).
    suspend fun addSource(
        modelId: Int,
        source: String,
        price: Double,
        currency: String,
        url: String,
        inStock: Int?,
        warehouse: String?,
        deliveryEstimate: String?,
        shippingAmount: Double?,
        shippingCurrency: String?,
    ): ApiResult<Model> = safeCall {
        api.addSource(modelId, AddSourceRequest(source, price, currency, url, inStock, warehouse)).close()
        applySourceOverride(modelId, source, inStock, warehouse, deliveryEstimate)
        applyShippingOverride(modelId, source, shippingAmount, shippingCurrency)
        api.getModel(modelId)
    }

    suspend fun updateSource(
        modelId: Int,
        sourceId: Int,
        sourceName: String,
        price: Double,
        currency: String,
        url: String,
        inStock: Int?,
        warehouse: String?,
        deliveryEstimate: String?,
        shippingAmount: Double?,
        shippingCurrency: String?,
    ): ApiResult<Model> = safeCall {
        api.updateSource(sourceId, UpdateSourceRequest(price, currency, url, inStock, warehouse)).close()
        applySourceOverride(modelId, sourceName, inStock, warehouse, deliveryEstimate)
        applyShippingOverride(modelId, sourceName, shippingAmount, shippingCurrency)
        api.getModel(modelId)
    }

    suspend fun deleteSource(modelId: Int, sourceId: Int): ApiResult<Model> = safeCall {
        api.deleteSource(sourceId).close()
        api.getModel(modelId)
    }

    // Tomt leveranstidsfält tar bort en ev. tidigare override helt (DELETE är en
    // no-op om ingen fanns) istället för att skicka en tom sträng till servern.
    private suspend fun applySourceOverride(
        modelId: Int,
        source: String,
        inStock: Int?,
        warehouse: String?,
        deliveryEstimate: String?,
    ) {
        if (deliveryEstimate.isNullOrBlank()) {
            api.deleteSourceOverride(modelId, source).close()
        } else {
            api.setSourceOverride(modelId, SourceOverrideRequest(source, inStock, warehouse, deliveryEstimate)).close()
        }
    }

    // Fraktkostnad (shipping_sek/shipping_override_*) ligger i en helt egen
    // tabell (model_shipping_overrides) från både prisraden OCH source-overriden
    // ovan — samma "tomt fält tar bort en ev. tidigare override"-princip som
    // leveranstid, se applySourceOverride.
    private suspend fun applyShippingOverride(
        modelId: Int,
        source: String,
        shippingAmount: Double?,
        shippingCurrency: String?,
    ) {
        if (shippingAmount == null) {
            api.deleteShippingOverride(modelId, source).close()
        } else {
            api.setShippingOverride(modelId, ShippingOverrideRequest(source, shippingAmount, shippingCurrency ?: "SEK")).close()
        }
    }

    // --- Issue #17 (mirroring mould-king-tracker issue #5) ---------------

    suspend fun updateBuildStatus(id: Int, buildStatus: String?): ApiResult<Model> = safeCall {
        api.updateBuildStatus(id, BuildStatusUpdateRequest(buildStatus))
    }

    suspend fun updateOrderNumber(id: Int, orderNumber: String): ApiResult<Model> = safeCall {
        api.updateOrderNumber(id, OrderNumberUpdateRequest(orderNumber))
    }

    suspend fun updateRebrickableSetNum(id: Int, rebrickableSetNum: String): ApiResult<Model> = safeCall {
        api.updateRebrickableSetNum(id, RebrickableSetNumUpdateRequest(rebrickableSetNum))
    }

    suspend fun uploadBuildPhoto(id: Int, photo: MultipartBody.Part): ApiResult<Model> = safeCall {
        api.uploadBuildPhoto(id, photo)
    }

    suspend fun deleteBuildPhoto(id: Int): ApiResult<Model> = safeCall {
        api.deleteBuildPhoto(id)
    }

    suspend fun getMissingParts(id: Int): ApiResult<MissingPartsResponse> = safeCall {
        api.getMissingParts(id)
    }

    suspend fun addMissingPart(
        id: Int,
        name: String,
        partNum: String?,
        colorName: String?,
        quantity: Int,
        sourceNote: String?,
    ): ApiResult<MissingPartsResponse> = safeCall {
        api.addMissingPart(id, AddMissingPartRequest(name, partNum, colorName, quantity, sourceNote)).close()
        api.getMissingParts(id)
    }

    suspend fun toggleMissingPartFound(id: Int, partId: Int, found: Boolean): ApiResult<MissingPartsResponse> = safeCall {
        api.toggleMissingPartFound(id, partId, ToggleMissingPartFoundRequest(found)).close()
        api.getMissingParts(id)
    }

    suspend fun deleteMissingPart(id: Int, partId: Int): ApiResult<MissingPartsResponse> = safeCall {
        api.deleteMissingPart(id, partId).close()
        api.getMissingParts(id)
    }

    // POST .../missing-parts/sync kräver rebrickable_set_num satt (400 annars)
    // och REBRICKABLE_*-nycklar konfigurerade server-side (503 annars) — se
    // parseErrorMessage/safeCall nedan för hur de felmeddelandena når fram.
    suspend fun syncMissingParts(id: Int): ApiResult<MissingPartsResponse> = safeCall {
        api.syncMissingParts(id).close()
        api.getMissingParts(id)
    }

    suspend fun getReceipts(id: Int): ApiResult<List<Receipt>> = safeCall {
        api.getReceipts(id)
    }

    suspend fun uploadReceipts(id: Int, files: List<MultipartBody.Part>): ApiResult<List<Receipt>> = safeCall {
        api.uploadReceipts(id, files).close()
        api.getReceipts(id)
    }

    suspend fun deleteReceipt(id: Int, receiptId: Int): ApiResult<List<Receipt>> = safeCall {
        api.deleteReceipt(receiptId).close()
        api.getReceipts(id)
    }

    // BackendResolver.ensureResolved() racear Tailscale-adressen mot den lokala hemma-IP:n
    // (issue #19 i Sivan87/BrickRadar) bara vid det allra första anropet i sessionen -
    // därefter är den ett omedelbart cache-svar, ingen omracing per anrop. Om den redan
    // valda adressen plötsligt slutar svara (IOException, t.ex. telefonen bytte nätverk
    // mitt i sessionen) racear vi om en gång och gör om anropet innan vi ger upp.
    private suspend inline fun <T> safeCall(resolveBackend: Boolean = true, crossinline block: suspend () -> T): ApiResult<T> {
        if (resolveBackend) BackendResolver.ensureResolved()
        return try {
            ApiResult.Success(block())
        } catch (e: HttpException) {
            ApiResult.Error(httpErrorMessage(e))
        } catch (e: IOException) {
            if (!resolveBackend) {
                return ApiResult.Error("Kunde inte nå GitHub")
            }
            // Den cachade adressen floppade plötsligt (t.ex. telefonen bytte nätverk mitt i
            // sessionen) - racea om en gång och gör om samma anrop innan vi ger upp.
            BackendResolver.resolve(force = true)
            try {
                ApiResult.Success(block())
            } catch (e2: HttpException) {
                ApiResult.Error(httpErrorMessage(e2))
            } catch (e2: IOException) {
                ApiResult.Error("Kunde inte nå servern — kontrollera Tailscale eller att telefonen är på hemma-WiFi")
            }
        }
    }

    private fun httpErrorMessage(e: HttpException): String = when (e.code()) {
        400 -> parseErrorMessage(e) ?: "Ogiltig data"
        401 -> "Ogiltig eller saknad API-nyckel"
        404 -> "Hittades inte"
        // 409: db.find_duplicate_model (api_add_model) — modellnummer+märke
        // matchar en befintlig modell. Servern inkluderar existing_model_id
        // i svaret men appen navigerar inte dit automatiskt (utanför scope
        // för Fas 6 — enbart manuell inmatning, ingen dubblettnavigering än).
        409 -> parseErrorMessage(e) ?: "Modellen finns redan"
        // 502/503: missing-parts/sync (Rebrickable-fel/inte konfigurerad,
        // se api.py: api_sync_missing_parts) — servern skickar alltid ett
        // specifikt {"error": "..."} för dessa, samma som 400/409 ovan.
        502, 503 -> parseErrorMessage(e) ?: "Serverfel (${e.code()})"
        else -> "Serverfel (${e.code()})"
    }

    private fun parseErrorMessage(e: HttpException): String? {
        val body = e.response()?.errorBody()?.string() ?: return null
        return try {
            RetrofitClient.moshi.adapter(ErrorResponse::class.java).fromJson(body)?.error
        } catch (parseError: Exception) {
            null
        }
    }
}

private data class ErrorResponse(@Json(name = "error") val error: String?)
