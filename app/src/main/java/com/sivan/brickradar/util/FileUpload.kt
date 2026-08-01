package com.sivan.brickradar.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Läser in en av användaren vald Uri (foto-/dokumentväljaren, ingen
// lagringsbehörighet krävs sedan Storage Access Framework/Photo Picker) helt
// i minnet och paketerar den som en multipart-del — build-photo/receipts är
// enstaka bilder/PDF:er, inte stora videofiler, så detta är inte ett problem
// i praktiken. Returnerar null om strömmen inte kan öppnas (t.ex. Uri:n har
// blivit ogiltig sedan den valdes).
fun uriToMultipartPart(contentResolver: ContentResolver, uri: Uri, partName: String): MultipartBody.Part? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
    val fileName = queryDisplayName(contentResolver, uri) ?: partName
    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, fileName, requestBody)
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }
    return null
}
