package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.core.network.dto.AddContactRequest
import com.gayathrini.chatapp.core.network.dto.AuthResponse
import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.CreateConversationRequest
import com.gayathrini.chatapp.core.network.dto.LoginRequest
import com.gayathrini.chatapp.core.network.dto.LogoutRequest
import com.gayathrini.chatapp.core.network.dto.MediaUploadResponse
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.MessagesPageDto
import com.gayathrini.chatapp.core.network.dto.ReadRequest
import com.gayathrini.chatapp.core.network.dto.RefreshRequest
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

    // Contacts
    @GET("contacts")
    suspend fun getContacts(): List<ContactDto>

    @POST("contacts")
    suspend fun addContact(@Body body: AddContactRequest): ContactDto

    @PATCH("contacts/{id}")
    suspend fun updateContact(@Path("id") id: String, @Body body: UpdateContactRequest): ContactDto

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String)

    // Conversations
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @POST("conversations")
    suspend fun createConversation(@Body body: CreateConversationRequest): ConversationDto

    @DELETE("conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String)

    @POST("conversations/{id}/read")
    suspend fun markRead(@Path("id") id: String, @Body body: ReadRequest)

    // Messages & media
    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("since") since: String? = null,
        @Query("limit") limit: Int? = null,
    ): MessagesPageDto

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body body: SendMessageRequest): MessageDto

    @Multipart
    @POST("media")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): MediaUploadResponse

    // Sync
    @GET("sync")
    suspend fun sync(@Query("since") since: String? = null): SyncResponse
}
