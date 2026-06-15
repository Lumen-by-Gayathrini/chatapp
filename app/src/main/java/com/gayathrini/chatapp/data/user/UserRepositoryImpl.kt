package com.gayathrini.chatapp.data.user

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.data.mapper.toUser
import com.gayathrini.chatapp.domain.model.User
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : UserRepository {

    override suspend fun getProfile(userId: String): AppResult<User> =
        safeApiCall(dispatchers, json) { api.getUserProfile(userId).toUser() }

    override suspend fun blockUser(userId: String): AppResult<Unit> =
        safeApiCall(dispatchers, json) { api.blockUser(userId) }

    override suspend fun unblockUser(userId: String): AppResult<Unit> =
        safeApiCall(dispatchers, json) { api.unblockUser(userId) }
}
