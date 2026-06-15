package com.gayathrini.chatapp.data.search

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.data.conversations.toDomain
import com.gayathrini.chatapp.data.conversations.toEntity
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.data.mapper.toUser
import com.gayathrini.chatapp.data.messages.toDomain
import com.gayathrini.chatapp.data.messages.toEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : SearchRepository {

    override suspend fun search(query: String): AppResult<SearchResults> {
        val q = query.trim()
        if (q.isBlank()) return AppResult.Success(SearchResults())
        val me = sessionStore.currentUserId()
        // The three scopes are independent endpoints — fan out in parallel (TDD §6.17).
        return safeApiCall(dispatchers, json) {
            coroutineScope {
                val messages = async { api.searchMessages(q) }
                val users = async { api.searchUsers(q) }
                val groups = async { api.searchGroups(q) }
                SearchResults(
                    messages = messages.await().map { it.toEntity().toDomain(me) },
                    users = users.await().map { it.toUser() },
                    groups = groups.await().map { it.toEntity().toDomain() },
                )
            }
        }
    }
}
