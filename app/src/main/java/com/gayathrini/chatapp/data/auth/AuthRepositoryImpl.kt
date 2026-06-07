package com.gayathrini.chatapp.data.auth

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.AuthResponse
import com.gayathrini.chatapp.core.network.dto.LoginRequest
import com.gayathrini.chatapp.core.network.dto.LogoutRequest
import com.gayathrini.chatapp.core.network.dto.RegisterRequest
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.data.local.Session
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.data.mapper.toUser
import com.gayathrini.chatapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = sessionStore.session.map { it != null }

    override suspend fun login(username: String, password: String): AppResult<User> =
        when (val result = safeApiCall(dispatchers, json) { api.login(LoginRequest(username, password)) }) {
            is AppResult.Success -> {
                persistSession(result.data)
                AppResult.Success(result.data.user.toUser())
            }
            is AppResult.Failure -> result
        }

    override suspend fun register(username: String, password: String, displayName: String): AppResult<User> =
        when (val result = safeApiCall(dispatchers, json) {
            api.register(RegisterRequest(username, password, displayName))
        }) {
            is AppResult.Success -> {
                persistSession(result.data)
                AppResult.Success(result.data.user.toUser())
            }
            is AppResult.Failure -> result
        }

    override suspend fun logout(): AppResult<Unit> {
        val refreshToken = sessionStore.refreshTokenBlocking()
        if (refreshToken != null) {
            // Best-effort server revoke; clearing the local session is what matters.
            runCatching { withContext(dispatchers.io) { api.logout(LogoutRequest(refreshToken)) } }
        }
        sessionStore.clear()
        return AppResult.Success(Unit)
    }

    override suspend fun currentUser(): AppResult<User> =
        safeApiCall(dispatchers, json) { api.getMe().toUser() }

    private suspend fun persistSession(auth: AuthResponse) {
        sessionStore.save(
            Session(
                accessToken = auth.accessToken,
                refreshToken = auth.refreshToken,
                userId = auth.user.id,
                displayName = auth.user.displayName,
            ),
        )
    }
}
