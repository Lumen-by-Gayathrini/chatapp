package com.gayathrini.chatapp.core.di

import com.gayathrini.chatapp.core.notifications.AndroidMessageNotifier
import com.gayathrini.chatapp.core.notifications.MessageNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the message notifier (TDD §6.6). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    @Singleton
    abstract fun bindMessageNotifier(impl: AndroidMessageNotifier): MessageNotifier
}
