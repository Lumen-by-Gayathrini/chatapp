package com.gayathrini.chatapp.data.profile

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.UpdateProfileRequest
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.data.mapper.toUser
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.data.media.MediaRepository
import com.gayathrini.chatapp.domain.model.User
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val mediaRepository: MediaRepository,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : ProfileRepository {

    override suspend fun load(): AppResult<User> =
        safeApiCall(dispatchers, json) { api.getMe().toUser() }

    override suspend fun updateDisplayName(displayName: String): AppResult<User> =
        safeApiCall(dispatchers, json) {
            api.updateMe(UpdateProfileRequest(displayName = displayName)).toUser()
        }

    override suspend fun updateAvatar(payload: MediaPayload): AppResult<User> =
        when (val upload = mediaRepository.upload(payload.bytes, payload.fileName, payload.mimeType)) {
            is AppResult.Failure -> upload
            is AppResult.Success -> safeApiCall(dispatchers, json) {
                api.updateMe(UpdateProfileRequest(avatarUrl = upload.data.url)).toUser()
            }
        }
}
