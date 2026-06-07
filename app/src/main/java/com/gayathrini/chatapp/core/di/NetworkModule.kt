package com.gayathrini.chatapp.core.di

import com.gayathrini.chatapp.BuildConfig
import com.gayathrini.chatapp.core.network.AuthInterceptor
import com.gayathrini.chatapp.core.network.AuthRefreshApi
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Builds the real Retrofit stack (TDD §7). It is wired but **dormant** during fake-API-first
 * development: nothing injects `@Named("remote") ChatApi`, so OkHttp/Retrofit are never constructed
 * at runtime. Phase 9 swaps the active [ChatApi] binding (see [ApiModule]) to this.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val AUTH = "auth"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // --- Bare client for token refresh (no auth) so the authenticator has no dependency cycle. ---

    @Provides
    @Singleton
    @Named(AUTH)
    fun provideAuthOkHttp(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(logging).build()

    @Provides
    @Singleton
    @Named(AUTH)
    fun provideAuthRetrofit(@Named(AUTH) client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthRefreshApi(@Named(AUTH) retrofit: Retrofit): AuthRefreshApi =
        retrofit.create(AuthRefreshApi::class.java)

    // --- Main authenticated client. ---

    @Provides
    @Singleton
    fun provideOkHttp(
        logging: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named("remote")
    fun provideRemoteChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
}
