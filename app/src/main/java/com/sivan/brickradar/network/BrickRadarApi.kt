package com.sivan.brickradar.network

import com.sivan.brickradar.model.AddMissingPartRequest
import com.sivan.brickradar.model.AddModelRequest
import com.sivan.brickradar.model.AddSourceRequest
import com.sivan.brickradar.model.Brick4SearchResult
import com.sivan.brickradar.model.BuildStatusUpdateRequest
import com.sivan.brickradar.model.CategoriesResponse
import com.sivan.brickradar.model.CategoryUpdateRequest
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.ModelUpdateRequest
import com.sivan.brickradar.model.MissingPartsResponse
import com.sivan.brickradar.model.OrderNumberUpdateRequest
import com.sivan.brickradar.model.RebrickableSetNumUpdateRequest
import com.sivan.brickradar.model.Receipt
import com.sivan.brickradar.model.SourceOverrideRequest
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.StatusUpdateRequest
import com.sivan.brickradar.model.ToggleMissingPartFoundRequest
import com.sivan.brickradar.model.UpdateSourceRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface BrickRadarApi {
    @GET("api/models")
    suspend fun getModels(
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("value_rating") valueRating: String? = null,
        @Query("sort") sort: String? = null,
    ): List<Model>

    @GET("api/models/{id}")
    suspend fun getModel(@Path("id") id: Int): Model

    // Returnerar den nyskapade modellen direkt (api_add_model: jsonify(db.get_model(model_id)), 201) —
    // till skillnad från källendpointerna behövs ingen separat omhämtning här.
    @POST("api/models")
    suspend fun addModel(@Body request: AddModelRequest): Model

    @GET("api/categories")
    suspend fun getCategories(): CategoriesResponse

    // Bara avg_kr_per_piece_clone_all/_lego_all (detaljvyns värde-skala) och
    // counts (filterchipsens siffror) används — se StatsResponse.
    @GET("api/stats")
    suspend fun getStats(): StatsResponse

    @DELETE("api/models/{id}")
    suspend fun deleteModel(@Path("id") id: Int): ResponseBody

    // En rad per märke med exakt modellnummerträff hos Brick4 — se
    // Brick4SearchResult. Tom lista om inget märke matchar modellnumret.
    @GET("api/brick4/search-by-number")
    suspend fun searchBrick4ByNumber(@Query("model_number") modelNumber: String): List<Brick4SearchResult>

    @PATCH("api/models/{id}/status")
    suspend fun updateStatus(@Path("id") id: Int, @Body request: StatusUpdateRequest): Model

    @PUT("api/models/{id}")
    suspend fun updateModel(@Path("id") id: Int, @Body request: ModelUpdateRequest): Model

    // Delvis uppdatering (bara category) — se CategoryUpdateRequest.
    @PUT("api/models/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body request: CategoryUpdateRequest): Model

    // Svaret (db.get_latest_prices, en lista utan kr_per_piece/best_-fält) används
    // aldrig direkt — ModelRepository hämtar alltid om hela modellen efteråt (se
    // kommentar där om varför: prices är append-only och overrides ligger i en
    // separat tabell, så bara ett omhämtat GET /models/{id} ger ett korrekt resultat).
    @POST("api/models/{id}/sources")
    suspend fun addSource(@Path("id") modelId: Int, @Body request: AddSourceRequest): ResponseBody

    @PUT("api/sources/{id}")
    suspend fun updateSource(@Path("id") sourceId: Int, @Body request: UpdateSourceRequest): ResponseBody

    @DELETE("api/sources/{id}")
    suspend fun deleteSource(@Path("id") sourceId: Int): ResponseBody

    @POST("api/models/{id}/source-override")
    suspend fun setSourceOverride(@Path("id") modelId: Int, @Body request: SourceOverrideRequest): ResponseBody

    @DELETE("api/models/{id}/source-override/{source}")
    suspend fun deleteSourceOverride(@Path("id") modelId: Int, @Path("source") source: String): ResponseBody

    // --- Issue #17 (mirroring mould-king-tracker issue #5) ---------------

    @PATCH("api/models/{id}/build-status")
    suspend fun updateBuildStatus(@Path("id") id: Int, @Body request: BuildStatusUpdateRequest): Model

    // Samma delade PUT /models/{id} som updateModel/updateCategory ovan, bara
    // med ett smalare body-fält var — se kommentaren vid CategoryUpdateRequest.
    @PUT("api/models/{id}")
    suspend fun updateOrderNumber(@Path("id") id: Int, @Body request: OrderNumberUpdateRequest): Model

    @PUT("api/models/{id}")
    suspend fun updateRebrickableSetNum(@Path("id") id: Int, @Body request: RebrickableSetNumUpdateRequest): Model

    @Multipart
    @POST("api/models/{id}/build-photo")
    suspend fun uploadBuildPhoto(@Path("id") id: Int, @Part photo: MultipartBody.Part): Model

    @DELETE("api/models/{id}/build-photo")
    suspend fun deleteBuildPhoto(@Path("id") id: Int): Model

    @GET("api/models/{id}/missing-parts")
    suspend fun getMissingParts(@Path("id") id: Int): MissingPartsResponse

    // Svarsformen (bara {"parts": [...]}, utan total/found_count/synced_at)
    // används aldrig direkt — ModelRepository hämtar alltid om hela
    // GET .../missing-parts efteråt, samma "trust the re-fetch, not the
    // mutation response" mönster som addSource/updateSource redan använder.
    @POST("api/models/{id}/missing-parts")
    suspend fun addMissingPart(@Path("id") id: Int, @Body request: AddMissingPartRequest): ResponseBody

    @PATCH("api/models/{id}/missing-parts/{partId}")
    suspend fun toggleMissingPartFound(
        @Path("id") id: Int,
        @Path("partId") partId: Int,
        @Body request: ToggleMissingPartFoundRequest,
    ): ResponseBody

    @DELETE("api/models/{id}/missing-parts/{partId}")
    suspend fun deleteMissingPart(@Path("id") id: Int, @Path("partId") partId: Int): ResponseBody

    @POST("api/models/{id}/missing-parts/sync")
    suspend fun syncMissingParts(@Path("id") id: Int): ResponseBody

    @GET("api/models/{id}/receipts")
    suspend fun getReceipts(@Path("id") id: Int): List<Receipt>

    // Fältnamn "files" (flera tillåtna i samma anrop) — se api.py:
    // api_upload_receipts.
    @Multipart
    @POST("api/models/{id}/receipts")
    suspend fun uploadReceipts(@Path("id") id: Int, @Part files: List<MultipartBody.Part>): ResponseBody

    @DELETE("api/receipts/{receiptId}")
    suspend fun deleteReceipt(@Path("receiptId") receiptId: Int): ResponseBody
}
