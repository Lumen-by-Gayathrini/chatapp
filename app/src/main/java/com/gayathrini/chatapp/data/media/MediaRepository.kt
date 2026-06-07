package com.gayathrini.chatapp.data.media

import com.gayathrini.chatapp.core.common.AppResult

/** A reference to an uploaded media object (TDD §7.6). */
data class MediaRef(val mediaId: String, val url: String)

/** Uploads media (photos) and returns the server reference used when sending an IMAGE message. */
interface MediaRepository {
    suspend fun upload(bytes: ByteArray, fileName: String, mimeType: String): AppResult<MediaRef>
}
