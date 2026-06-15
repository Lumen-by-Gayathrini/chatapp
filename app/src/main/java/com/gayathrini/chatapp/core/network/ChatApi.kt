package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.core.network.dto.AddContactRequest
import com.gayathrini.chatapp.core.network.dto.AddMemberRequest
import com.gayathrini.chatapp.core.network.dto.AuthResponse
import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.CreateConversationRequest
import com.gayathrini.chatapp.core.network.dto.CreateGroupRequest
import com.gayathrini.chatapp.core.network.dto.EditMessageRequest
import com.gayathrini.chatapp.core.network.dto.UpdateGroupRequest
import com.gayathrini.chatapp.core.network.dto.LoginRequest
import com.gayathrini.chatapp.core.network.dto.LogoutRequest
import com.gayathrini.chatapp.core.network.dto.MediaUploadResponse
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.MessagesPageDto
import com.gayathrini.chatapp.core.network.dto.MuteRequest
import com.gayathrini.chatapp.core.network.dto.ReactionRequest
import com.gayathrini.chatapp.core.network.dto.ReadRequest
import com.gayathrini.chatapp.core.network.dto.RefreshRequest
import com.gayathrini.chatapp.core.network.dto.SetDisappearingRequest
import com.gayathrini.chatapp.core.network.dto.RegisterRequest
import com.gayathrini.chatapp.core.network.dto.SendMessageRequest
import com.gayathrini.chatapp.core.network.dto.SyncResponse
import com.gayathrini.chatapp.core.network.dto.TokenResponse
import com.gayathrini.chatapp.core.network.dto.UpdateContactRequest
import com.gayathrini.chatapp.core.network.dto.UpdateProfileRequest
import com.gayathrini.chatapp.core.network.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The single API surface the repositories depend on (TDD §7). It is a Retrofit interface, so the
 * real implementation is `retrofit.create(ChatApi::class.java)`; [com.gayathrini.chatapp.data.remote.FakeChatApi]
 * implements the same interface in-memory for fake-API-first development (plan §8).
 */
interface ChatApi {

    // Auth & profile
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @GET("me")
    suspend fun getMe(): UserDto

    @PATCH("me")
    suspend fun updateMe(@Body body: UpdateProfileRequest): UserDto

    /** Another participant's public profile (TDD §6.2). */
    @GET("users/{id}/profile")
    suspend fun getUserProfile(@Path("id") id: String): UserDto

    /** Presence heartbeat (TDD §6.5) — stamps the caller's `lastSeenAt`. */
    @POST("presence/ping")
    suspend fun pingPresence()

    // Blocking (TDD §6.19).
    @POST("users/{id}/block")
    suspend fun blockUser(@Path("id") id: String)

    @DELETE("users/{id}/block")
    suspend fun unblockUser(@Path("id") id: String)

    @GET("users/blocked")
    suspend fun getBlockedUsers(): List<UserDto>

    // Contacts
    @GET("contacts")
    suspend fun getContacts(): List<ContactDto>

    @POST("contacts")
    suspend fun addContact(@Body body: AddContactRequest): ContactDto

    @PATCH("contacts/{id}")
    suspend fun updateContact(@Path("id") id: String, @Body body: UpdateContactRequest): ContactDto

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String)

    // Conversations. `archived=true` returns the Archived view; omit it for the main list (§6.23).
    @GET("conversations")
    suspend fun getConversations(@Query("archived") archived: Boolean? = null): List<ConversationDto>

    @POST("conversations")
    suspend fun createConversation(@Body body: CreateConversationRequest): ConversationDto

    @DELETE("conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String)

    // Groups (TDD §6.15).
    @POST("groups")
    suspend fun createGroup(@Body body: CreateGroupRequest): ConversationDto

    @POST("conversations/{id}/members")
    suspend fun addMember(@Path("id") id: String, @Body body: AddMemberRequest): ConversationDto

    @DELETE("conversations/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)

    @PATCH("conversations/{id}")
    suspend fun updateGroup(@Path("id") id: String, @Body body: UpdateGroupRequest): ConversationDto

    /** Set/clear disappearing-messages TTL (TDD §6.25); shares the conversation PATCH endpoint. */
    @PATCH("conversations/{id}")
    suspend fun setDisappearing(
        @Path("id") id: String,
        @Body body: SetDisappearingRequest,
    ): ConversationDto

    @POST("conversations/{id}/read")
    suspend fun markRead(@Path("id") id: String, @Body body: ReadRequest)

    // Mute / unmute (TDD §6.18) — both return the updated conversation.
    @POST("conversations/{id}/mute")
    suspend fun muteConversation(@Path("id") id: String, @Body body: MuteRequest): ConversationDto

    @POST("conversations/{id}/unmute")
    suspend fun unmuteConversation(@Path("id") id: String): ConversationDto

    // Pin / unpin (TDD §6.22) — both return the updated conversation.
    @POST("conversations/{id}/pin")
    suspend fun pinConversation(@Path("id") id: String): ConversationDto

    @POST("conversations/{id}/unpin")
    suspend fun unpinConversation(@Path("id") id: String): ConversationDto

    // Archive / unarchive (TDD §6.23) — both return the updated conversation.
    @POST("conversations/{id}/archive")
    suspend fun archiveConversation(@Path("id") id: String): ConversationDto

    @POST("conversations/{id}/unarchive")
    suspend fun unarchiveConversation(@Path("id") id: String): ConversationDto

    /** Typing heartbeat (TDD §6.10) — re-emitted ~every 3s while the user is typing. */
    @POST("conversations/{id}/typing")
    suspend fun sendTyping(@Path("id") id: String)

    /** Media gallery (TDD §6.16): the conversation's IMAGE/FILE messages. */
    @GET("conversations/{id}/media")
    suspend fun getConversationMedia(
        @Path("id") id: String,
        @Query("type") type: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int? = null,
    ): MessagesPageDto

    // Messages & media
    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("since") since: String? = null,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int? = null,
    ): MessagesPageDto

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body body: SendMessageRequest): MessageDto

    // Reactions (TDD §6.11) — both return the updated message.
    @POST("conversations/{id}/messages/{mid}/reactions")
    suspend fun addReaction(
        @Path("id") id: String,
        @Path("mid") mid: String,
        @Body body: ReactionRequest,
    ): MessageDto

    @DELETE("conversations/{id}/messages/{mid}/reactions")
    suspend fun removeReaction(@Path("id") id: String, @Path("mid") mid: String): MessageDto

    // Editing (TDD §6.21) — returns the updated message.
    @PATCH("conversations/{id}/messages/{mid}")
    suspend fun editMessage(
        @Path("id") id: String,
        @Path("mid") mid: String,
        @Body body: EditMessageRequest,
    ): MessageDto

    // Star / unstar (TDD §6.24) — both return the updated message.
    @POST("conversations/{id}/messages/{mid}/star")
    suspend fun starMessage(@Path("id") id: String, @Path("mid") mid: String): MessageDto

    @DELETE("conversations/{id}/messages/{mid}/star")
    suspend fun unstarMessage(@Path("id") id: String, @Path("mid") mid: String): MessageDto

    /** The caller's starred messages (TDD §6.24). */
    @GET("starred")
    suspend fun getStarredMessages(): List<MessageDto>

    // Deletion (TDD §6.14).
    @DELETE("conversations/{id}/messages/{mid}")
    suspend fun deleteForEveryone(@Path("id") id: String, @Path("mid") mid: String): MessageDto

    @POST("conversations/{id}/messages/{mid}/hide")
    suspend fun hideMessage(@Path("id") id: String, @Path("mid") mid: String)

    @Multipart
    @POST("media")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): MediaUploadResponse

    // Search (TDD §6.17) — full-text over the caller's messages, users, and the caller's groups.
    @GET("search/messages")
    suspend fun searchMessages(@Query("q") q: String): List<MessageDto>

    @GET("search/users")
    suspend fun searchUsers(@Query("q") q: String): List<UserDto>

    @GET("search/groups")
    suspend fun searchGroups(@Query("q") q: String): List<ConversationDto>

    // Sync
    @GET("sync")
    suspend fun sync(@Query("since") since: String? = null): SyncResponse
}
