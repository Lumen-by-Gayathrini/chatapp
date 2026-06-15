package com.gayathrini.chatapp.ui.group

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.contacts.ContactRepository
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.domain.model.Contact
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.ConversationType
import com.gayathrini.chatapp.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModelTest {

    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val conversationRepository = mockk<ConversationRepository>()

    private val john = Contact("c_john", User("u_john", "john", "John"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { contactRepository.contacts } returns flowOf(listOf(john))
        coEvery { contactRepository.refresh() } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun create_disabledUntilTitleAndMemberChosen() {
        val vm = CreateGroupViewModel(contactRepository, conversationRepository)
        assertEquals(false, vm.state.value.canCreate)
        vm.onTitleChange("Team")
        assertEquals(false, vm.state.value.canCreate) // no members yet
        vm.toggleMember("u_john")
        assertEquals(true, vm.state.value.canCreate)
    }

    @Test
    fun create_callsRepository_andEmitsCreated() = runTest {
        coEvery { conversationRepository.createGroup("Team", listOf("u_john")) } returns
            AppResult.Success(
                Conversation("g1", null, null, null, 0, type = ConversationType.GROUP, title = "Team"),
            )
        val vm = CreateGroupViewModel(contactRepository, conversationRepository)
        vm.onTitleChange("Team")
        vm.toggleMember("u_john")

        vm.effects.test {
            vm.create()
            assertEquals(CreateGroupEffect.Created("g1"), awaitItem())
        }
        coVerify { conversationRepository.createGroup("Team", listOf("u_john")) }
    }
}
