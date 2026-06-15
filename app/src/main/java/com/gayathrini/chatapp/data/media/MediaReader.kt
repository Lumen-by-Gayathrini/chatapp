package com.gayathrini.chatapp.data.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.gayathrini.chatapp.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Raw bytes + metadata of a picked photo or document, ready to upload (TDD §6.7). */
data class MediaPayload(val bytes: ByteArray, val fileName: String, val mimeType: String)

/** Reads a picked content [Uri] into bytes off the main thread. */
interface MediaReader {
    suspend fun read(uri: Uri): MediaPayload?
}

class AndroidMediaReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : MediaReader {

    override suspend fun read(uri: Uri): MediaPayload? = withContext(dispatchers.io) {
        runCatching {
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            // Prefer the real document name (so files keep e.g. "report.pdf"); else synthesize one.
            val extension = mimeType.substringAfter('/', "bin")
            val fileName = displayName(uri) ?: "upload_${System.currentTimeMillis()}.$extension"
            MediaPayload(bytes, fileName, mimeType)
        }.getOrNull()
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
