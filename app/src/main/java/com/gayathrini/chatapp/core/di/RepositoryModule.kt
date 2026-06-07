package com.gayathrini.chatapp.core.di

import com.gayathrini.chatapp.data.auth.AuthRepository
import com.gayathrini.chatapp.data.auth.AuthRepositoryImpl
import com.gayathrini.chatapp.data.contacts.ContactRepository
import com.gayathrini.chatapp.data.contacts.ContactRepositoryImpl
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.conversations.ConversationRepositoryImpl
import com.gayathrini.chatapp.data.media.AndroidMediaReader
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.media.MediaRepository
import com.gayathrini.chatapp.data.media.MediaRepositoryImpl
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.data.messages.MessageRepositoryImpl
import com.gayathrini.chatapp.data.profile.ProfileRepository
import com.gayathrini.chatapp.data.profile.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces to their implementations. Grows one binding per feature slice. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindMediaReader(impl: AndroidMediaReader): MediaReader

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
