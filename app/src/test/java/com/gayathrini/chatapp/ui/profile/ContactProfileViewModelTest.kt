package com.gayathrini.chatapp.ui.profile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.user.UserRepository
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ContactProfileViewModelTest {

    private val userRepository = mockk<UserRepository>()
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)

    private val john = User("u_john", "john", "John", null, "Loves gardening")

    private fun viewModel() = ContactProfileViewModel(
        SavedStateHandle(mapOf(Screen.ContactProfile.ARG to "u_john")),
        userRepository,
        conversationRepository,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { userRepository.getProfile("u_john") } returns AppResult.Success(john)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsProfileFromRepository() {
        val vm = viewModel()

        assertEquals(false, vm.state.value.isLoading)
        assertEquals(john, vm.state.value.user)
        coVerify { userRepository.getProfile("u_john") }
    }

    @Test
    fun load_failure_setsError() {
        coEvery { userRepository.getProfile("u_john") } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()

        assertNull(vm.state.value.user)
        assertEquals(false, vm.state.value.isLoading)
        assertEquals(true, vm.state.value.error != null)
    }

    @Test
    fun startChat_createsConversation_andEmitsOpen() = runTest {
        val conversation = Conversation("conv_john", john, null, null, 0)
        coEvery { conversationRepository.createConversation("u_john") } returns AppResult.Success(conversation)
        val vm = viewModel()

        vm.effects.test {
            vm.startChat()
            assertEquals(ContactProfileEffect.OpenConversation("conv_john"), awaitItem())
        }
        coVerify { conversationRepository.createConversation("u_john") }
    }

    @Test
    fun startChat_failure_setsErrorAndNoEffect() = runTest {
        coEvery { conversationRepository.createConversation("u_john") } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.startChat()

        assertEquals(false, vm.state.value.isStartingChat)
        assertEquals(true, vm.state.value.error != null)
    }

    @Test
    fun toggleBlock_blocksThenReloads_reflectingBlockedState() {
        // Init load returns the un-blocked profile; the post-block reload returns the blocked one.
        coEvery { userRepository.getProfile("u_john") } returns
            AppResult.Success(john) andThen AppResult.Success(john.copy(blocked = true))
        coEvery { userRepository.blockUser("u_john") } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.toggleBlock()

        coVerify { userRepository.blockUser("u_john") }
        assertEquals(true, vm.state.value.isBlocked)
    }

    @Test
    fun toggleBlock_whenBlocked_unblocks() {
        coEvery { userRepository.getProfile("u_john") } returns AppResult.Success(john.copy(blocked = true))
        coEvery { userRepository.unblockUser("u_john") } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.toggleBlock()

        coVerify { userRepository.unblockUser("u_john") }
    }
}
