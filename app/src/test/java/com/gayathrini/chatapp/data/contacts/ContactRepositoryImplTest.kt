package com.gayathrini.chatapp.data.contacts

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.core.network.dto.UserDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRepositoryImplTest {

    private val api = mockk<ChatApi>()
    private val dao = mockk<ContactDao>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }
    private val repo by lazy { ContactRepositoryImpl(api, dao, dispatchers, Json { ignoreUnknownKeys = true }) }

    @Before
    fun setUp() {
        every { dao.observeAll() } returns flowOf(emptyList())
    }

    @Test
    fun refresh_success_replacesCache() = runTest {
        coEvery { api.getContacts() } returns listOf(ContactDto("c1", UserDto("u1", "john", "John"), null))

        val result = repo.refresh()

        assertTrue(result is AppResult.Success)
        coVerify { dao.replaceAll(match { it.size == 1 && it.first().id == "c1" }) }
    }

    @Test
    fun addContact_success_upsertsAndReturnsDomain() = runTest {
        coEvery { api.addContact(any()) } returns ContactDto("c2", UserDto("u2", "emma", "Emma"), null)

        val result = repo.addContact("emma")

        assertTrue(result is AppResult.Success)
        assertEquals("Emma", (result as AppResult.Success).data.displayName)
        coVerify { dao.upsert(match { it.id == "c2" }) }
    }

    @Test
    fun contacts_mapsCachedEntitiesToDomain() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(ContactEntity("c1", "u1", "john", "John", null, null)),
        )

        val contacts = repo.contacts.first()

        assertEquals(1, contacts.size)
        assertEquals("John", contacts.first().displayName)
    }
}
