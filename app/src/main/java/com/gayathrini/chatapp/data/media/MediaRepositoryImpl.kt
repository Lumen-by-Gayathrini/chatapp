package com.gayathrini.chatapp.data.media

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.safeApiCall
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : MediaRepository {

    override suspend fun upload(bytes: ByteArray, fileName: String, mimeType: String): AppResult<MediaRef> =
        safeApiCall(dispatchers, json) {
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", fileName, body)
            val response = api.uploadMedia(part)
            MediaRef(response.mediaId, response.url)
        }
}
