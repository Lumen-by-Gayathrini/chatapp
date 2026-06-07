package com.gayathrini.chatapp.data.auth

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Authentication + session management (TDD §5.1, §7.2). */
interface AuthRepository {
    /** Emits whether a session is currently stored (drives cold-start routing). */
    val isLoggedIn: Flow<Boolean>

    suspend fun login(username: String, password: String): AppResult<User>

    suspend fun register(username: String, password: String, displayName: String): AppResult<User>

    suspend fun logout(): AppResult<Unit>

    suspend fun currentUser(): AppResult<User>
}
