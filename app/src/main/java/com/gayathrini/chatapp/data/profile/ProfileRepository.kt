package com.gayathrini.chatapp.data.profile

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.domain.model.User

/** The signed-in user's profile (TDD §5.5): `GET`/`PATCH /me` plus avatar upload. */
interface ProfileRepository {
    suspend fun load(): AppResult<User>

    suspend fun updateDisplayName(displayName: String): AppResult<User>

    suspend fun updateAvatar(payload: MediaPayload): AppResult<User>
}
