package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.core.network.dto.RefreshRequest
import com.gayathrini.chatapp.core.network.dto.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * A minimal, auth-free refresh endpoint used by [TokenAuthenticator]. It runs on a bare OkHttp
 * client (no auth interceptor/authenticator) to avoid the classic refresh dependency cycle, and
 * returns a synchronous [Call] because [okhttp3.Authenticator] is blocking.
 */
interface AuthRefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokenResponse>
}
