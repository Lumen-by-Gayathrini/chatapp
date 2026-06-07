package com.gayathrini.chatapp.core.di

import android.content.Context
import androidx.room.Room
import com.gayathrini.chatapp.data.contacts.ContactDao
import com.gayathrini.chatapp.data.conversations.ConversationDao
import com.gayathrini.chatapp.data.local.ChatDatabase
import com.gayathrini.chatapp.data.messages.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase =
        Room.databaseBuilder(context, ChatDatabase::class.java, "chat.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideContactDao(database: ChatDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideConversationDao(database: ChatDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: ChatDatabase): MessageDao = database.messageDao()
}
