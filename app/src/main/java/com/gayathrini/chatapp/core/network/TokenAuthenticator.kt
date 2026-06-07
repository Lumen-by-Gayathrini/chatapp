package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.core.network.dto.RefreshRequest
import com.gayathrini.chatapp.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On a `401`, attempts a single token refresh and retries the original request; if refresh fails
 * it clears the session so the app routes back to Login (TDD §7.2). Used only by the real client.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRefreshApi: AuthRefreshApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only retry once.
        if (responseCount(response) >= 2) return null

        val refreshToken = sessionStore.refreshTokenBlocking() ?: return null

        val refreshed = runCatching { authRefreshApi.refresh(RefreshRequest(refreshToken)).execute() }
            .getOrNull()

        val body = refreshed?.takeIf { it.isSuccessful }?.body()
        if (body == null) {
            runBlocking { sessionStore.clear() }
            return null
        }

        runBlocking { sessionStore.updateTokens(body.accessToken, body.refreshToken) }
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${body.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 1
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
