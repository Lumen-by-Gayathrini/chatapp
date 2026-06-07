package com.gayathrini.chatapp.core.di

import com.gayathrini.chatapp.BuildConfig
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.data.remote.FakeChatApi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Selects the active [ChatApi]. Phase 9 wires the real `@Named("remote")` Retrofit implementation
 * (against the deployed chatapp-server); setting `USE_FAKE_API = true` falls back to the in-memory
 * [FakeChatApi] for offline development. `Lazy` ensures only the chosen stack is ever constructed.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideChatApi(
        fake: Lazy<FakeChatApi>,
        @Named("remote") remote: Lazy<ChatApi>,
    ): ChatApi = if (BuildConfig.USE_FAKE_API) fake.get() else remote.get()
}
