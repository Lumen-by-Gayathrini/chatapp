package com.gayathrini.chatapp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** The persisted researcher/participant session (TDD §6.3). */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val displayName: String,
)

/**
 * Stores the auth session in a Preferences DataStore (TDD §6.3). Exposes a reactive [session]
 * for the launch router, plus blocking token reads for the OkHttp interceptor/authenticator.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val access = prefs[Keys.ACCESS]
        val refresh = prefs[Keys.REFRESH]
        if (access.isNullOrEmpty() || refresh.isNullOrEmpty()) {
            null
        } else {
            Session(
                accessToken = access,
                refreshToken = refresh,
                userId = prefs[Keys.USER_ID].orEmpty(),
                displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            )
        }
    }

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = session.accessToken
            prefs[Keys.REFRESH] = session.refreshToken
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.DISPLAY_NAME] = session.displayName
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            prefs[Keys.REFRESH] = refreshToken
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun currentUserId(): String? = session.first()?.userId

    fun accessTokenBlocking(): String? = runBlocking { dataStore.data.first()[Keys.ACCESS] }

    fun refreshTokenBlocking(): String? = runBlocking { dataStore.data.first()[Keys.REFRESH] }
}
