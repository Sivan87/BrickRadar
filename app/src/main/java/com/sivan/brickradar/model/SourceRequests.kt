package com.sivan.brickradar.model

import com.squareup.moshi.Json

data class AddSourceRequest(
    val source: String,
    val price: Double,
    val currency: String,
    val url: String,
    @Json(name = "in_stock") val inStock: Int?,
    val warehouse: String?,
    val locked: Boolean = false,
)

data class UpdateSourceRequest(
    val price: Double,
    val currency: String,
    val url: String,
    @Json(name = "in_stock") val inStock: Int?,
    val warehouse: String?,
    val locked: Boolean,
)

// POST/DELETE .../source-override — separat lagringsplats (model_source_overrides
// i databasen) från själva prisraden. Leveranstid (delivery_estimate) går ALDRIG
// att sätta via POST .../sources eller PUT .../sources/{id} (prices-tabellen har
// ingen delivery_estimate-kolumn) — se kommentaren i ModelRepository.addSource.
data class SourceOverrideRequest(
    val source: String,
    @Json(name = "in_stock") val inStock: Int?,
    val warehouse: String?,
    @Json(name = "delivery_estimate") val deliveryEstimate: String?,
)

// POST/DELETE .../shipping-override — egen lagringsplats (model_shipping_overrides),
// helt separat från source-override ovan (som bara har in_stock/warehouse/
// delivery_estimate). Frakt kan alltså vara i en annan valuta än själva priset,
// se webb-UI:ts egen "Faktisk fraktkostnad"-dialog (static/app.js).
data class ShippingOverrideRequest(
    val source: String,
    val amount: Double,
    val currency: String,
)
