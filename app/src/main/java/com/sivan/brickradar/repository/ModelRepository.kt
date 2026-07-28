package com.sivan.brickradar.repository

import com.sivan.brickradar.model.AddModelRequest
import com.sivan.brickradar.model.AddSourceRequest
import com.sivan.brickradar.model.AppVersionResponse
import com.sivan.brickradar.model.Brick4SearchResult
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.CategoryUpdateRequest
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.ModelUpdateRequest
import com.sivan.brickradar.model.SourceOverrideRequest
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.StatusUpdateRequest
import com.sivan.brickradar.model.UpdateSourceRequest
import com.sivan.brickradar.network.BrickRadarApi
import com.sivan.brickradar.network.RetrofitClient
import com.squareup.moshi.Json
import java.io.IOException
import retrofit2.HttpException

class ModelRepository(
    private val api: BrickRadarApi = RetrofitClient.api,
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

    // Fail-silent-kravet (uppdateringskollen ska aldrig störa vanlig
    // appanvändning om servern inte är nåbar) hanteras av ANROPAREN
    // (UpdateViewModel ignorerar ApiResult.Error helt) — safeCall/ApiResult
    // är samma väg som resten av repositoryt, ingen särbehandling behövs här.
    suspend fun getAppVersion(): ApiResult<AppVersionResponse> = safeCall {
        api.getAppVersion()
    }

    suspend fun deleteModel(id: Int): ApiResult<Unit> = safeCall {
        api.deleteModel(id).close()
    }

    suspend fun updateStatus(id: Int, status: String): ApiResult<Model> = safeCall {
        api.updateStatus(id, StatusUpdateRequest(status))
    }

    suspend fun updateModel(id: Int, name: String, pieceCount: Int, category: String): ApiResult<Model> = safeCall {
        api.updateModel(id, ModelUpdateRequest(name, pieceCount, category))
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
    ): ApiResult<Model> = safeCall {
        api.addSource(modelId, AddSourceRequest(source, price, currency, url, inStock, warehouse)).close()
        applySourceOverride(modelId, source, inStock, warehouse, deliveryEstimate)
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
    ): ApiResult<Model> = safeCall {
        api.updateSource(sourceId, UpdateSourceRequest(price, currency, url, inStock, warehouse)).close()
        applySourceOverride(modelId, sourceName, inStock, warehouse, deliveryEstimate)
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

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (e: HttpException) {
            val message = when (e.code()) {
                400 -> parseErrorMessage(e) ?: "Ogiltig data"
                401 -> "Ogiltig eller saknad API-nyckel"
                404 -> "Hittades inte"
                // 409: db.find_duplicate_model (api_add_model) — modellnummer+märke
                // matchar en befintlig modell. Servern inkluderar existing_model_id
                // i svaret men appen navigerar inte dit automatiskt (utanför scope
                // för Fas 6 — enbart manuell inmatning, ingen dubblettnavigering än).
                409 -> parseErrorMessage(e) ?: "Modellen finns redan"
                else -> "Serverfel (${e.code()})"
            }
            ApiResult.Error(message)
        } catch (e: IOException) {
            ApiResult.Error("Kunde inte nå servern — kontrollera att telefonen är på samma WiFi")
        }
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
