package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.data.local.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds `Authorization: Bearer <accessToken>` to outgoing requests when a session exists (TDD §7.2).
 * Only used by the real (Retrofit) client; the fake API ignores it.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.accessTokenBlocking()
        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
