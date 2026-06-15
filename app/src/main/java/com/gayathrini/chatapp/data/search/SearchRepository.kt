package com.gayathrini.chatapp.data.search

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.User

/** Combined results of a single search across the three scopes (TDD §6.17). */
data class SearchResults(
    val messages: List<Message> = emptyList(),
    val users: List<User> = emptyList(),
    val groups: List<Conversation> = emptyList(),
) {
    val isEmpty: Boolean get() = messages.isEmpty() && users.isEmpty() && groups.isEmpty()
}

/** Full-text search over the caller's messages, users, and the caller's groups (TDD §6.17). */
interface SearchRepository {
    suspend fun search(query: String): AppResult<SearchResults>
}
