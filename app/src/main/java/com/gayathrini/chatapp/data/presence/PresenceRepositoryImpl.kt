package com.gayathrini.chatapp.data.presence

import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.data.local.SessionStore
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : PresenceRepository {

    override suspend fun heartbeat() {
        // Don't ping while signed out (the endpoint is auth-gated). Best-effort: ignore the result.
        if (sessionStore.currentUserId() == null) return
        safeApiCall(dispatchers, json) { api.pingPresence() }
    }
}
