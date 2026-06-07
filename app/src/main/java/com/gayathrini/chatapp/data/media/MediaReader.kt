package com.gayathrini.chatapp.data.media

import android.content.Context
import android.net.Uri
import com.gayathrini.chatapp.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Raw bytes + metadata of a picked photo, ready to upload. */
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
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val extension = mimeType.substringAfter('/', "jpg")
            MediaPayload(bytes, "upload_${System.currentTimeMillis()}.$extension", mimeType)
        }.getOrNull()
    }
}
