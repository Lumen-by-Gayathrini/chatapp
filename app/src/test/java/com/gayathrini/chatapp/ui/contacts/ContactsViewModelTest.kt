package com.gayathrini.chatapp.ui.contacts

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.contacts.ContactRepository
import com.gayathrini.chatapp.domain.model.Contact
import com.gayathrini.chatapp.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    private val repository = mockk<ContactRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repository.refresh() } returns AppResult.Success(Unit)
        every { repository.contacts } returns flowOf(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsContacts_andFiltersByQuery() {
        every { repository.contacts } returns flowOf(
            listOf(
                Contact("c1", User("u1", "john", "John")),
                Contact("c2", User("u2", "emma", "Emma")),
            ),
        )
        val viewModel = ContactsViewModel(repository)

        assertEquals(2, viewModel.state.value.contacts.size)

        viewModel.onQueryChange("emma")
        assertEquals(1, viewModel.state.value.contacts.size)
        assertEquals("Emma", viewModel.state.value.contacts.first().displayName)
    }

    @Test
    fun submitAdd_withBlankUsername_setsError() {
        val viewModel = ContactsViewModel(repository)

        viewModel.openAddDialog()
        viewModel.submitAdd()

        assertEquals("Please enter a username.", viewModel.state.value.addError)
    }

    @Test
    fun onContactClick_emitsOpenConversation() = runTest {
        val john = Contact("c1", User("u1", "john", "John"))
        every { repository.contacts } returns flowOf(listOf(john))
        val viewModel = ContactsViewModel(repository)

        viewModel.effects.test {
            viewModel.onContactClick(john)
            assertEquals(ContactsEffect.OpenConversation("u1"), awaitItem())
        }
    }

    @Test
    fun confirmRemove_callsRepository_andClearsPending() {
        val contact = Contact("c1", User("u1", "john", "John"))
        every { repository.contacts } returns flowOf(listOf(contact))
        coEvery { repository.removeContact("c1") } returns AppResult.Success(Unit)
        val viewModel = ContactsViewModel(repository)

        viewModel.requestRemove(contact)
        assertEquals(contact, viewModel.state.value.pendingRemoval)

        viewModel.confirmRemove()
        coVerify { repository.removeContact("c1") }
        assertNull(viewModel.state.value.pendingRemoval)
    }
}
