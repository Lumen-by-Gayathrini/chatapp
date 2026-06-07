package com.gayathrini.chatapp.data.auth

import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.AuthResponse
import com.gayathrini.chatapp.core.network.dto.UserDto
import com.gayathrini.chatapp.data.local.SessionStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val api = mockk<ChatApi>()
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }
    private val repository = AuthRepositoryImpl(api, sessionStore, dispatchers, Json { ignoreUnknownKeys = true })

    @Test
    fun login_success_persistsSession_andReturnsUser() = runTest {
        coEvery { api.login(any()) } returns
            AuthResponse(UserDto("u1", "mary", "Mary"), "access-1", "refresh-1")

        val result = repository.login("mary", "pw")

        assertTrue(result is AppResult.Success)
        assertEquals("Mary", (result as AppResult.Success).data.displayName)
        coVerify { sessionStore.save(match { it.accessToken == "access-1" && it.userId == "u1" }) }
    }

    @Test
    fun login_networkFailure_mapsToNetworkError() = runTest {
        coEvery { api.login(any()) } throws IOException("no connection")

        val result = repository.login("mary", "pw")

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Network, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { sessionStore.save(any()) }
    }

    @Test
    fun logout_clearsSession() = runTest {
        every { sessionStore.refreshTokenBlocking() } returns "refresh-1"
        coEvery { api.logout(any()) } returns Unit

        repository.logout()

        coVerify { sessionStore.clear() }
    }

    @Test
    fun register_success_persistsSession() = runTest {
        coEvery { api.register(any()) } returns
            AuthResponse(UserDto("u1", "mary", "Mary"), "access-1", "refresh-1")

        val result = repository.register("mary", "pw", "Mary")

        assertTrue(result is AppResult.Success)
        coVerify { sessionStore.save(match { it.userId == "u1" }) }
    }
}
