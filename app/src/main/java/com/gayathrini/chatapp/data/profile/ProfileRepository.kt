package com.gayathrini.chatapp.data.profile

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.domain.model.User

/** The signed-in user's profile (TDD §5.5): `GET`/`PATCH /me` plus avatar upload. */
interface ProfileRepository {
    suspend fun load(): AppResult<User>

    /** Update the editable profile text — display name and status/about (TDD §6.1). */
    suspend fun updateProfile(displayName: String, about: String): AppResult<User>

    /** Toggle the "last seen & online" privacy setting (TDD §6.5). */
    suspend fun setShowLastSeen(enabled: Boolean): AppResult<User>

    suspend fun updateAvatar(payload: MediaPayload): AppResult<User>
}
