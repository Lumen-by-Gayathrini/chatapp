package com.gayathrini.chatapp.data.user

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.domain.model.User

/** Reads other participants' public profiles (TDD §6.2): `GET /users/:id/profile`. */
interface UserRepository {
    suspend fun getProfile(userId: String): AppResult<User>

    /** Block a user (TDD §6.19). */
    suspend fun blockUser(userId: String): AppResult<Unit>

    /** Remove the block on a user (TDD §6.19). */
    suspend fun unblockUser(userId: String): AppResult<Unit>
}
