package com.gayathrini.chatapp.ui.profile

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.auth.AuthRepository
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.profile.ProfileRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val profileRepository = mockk<ProfileRepository>()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)

    private fun viewModel() = ProfileViewModel(profileRepository, authRepository, mediaReader)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { profileRepository.load() } returns AppResult.Success(User("u1", "mary", "Mary"))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsProfileOnInit() {
        val vm = viewModel()
        assertEquals("Mary", vm.state.value.displayName)
        assertEquals("mary", vm.state.value.username)
    }

    @Test
    fun save_withBlankName_setsError() {
        val vm = viewModel()
        vm.onDisplayNameChange("   ")
        vm.save()
        assertEquals("Please enter your name.", vm.state.value.error)
    }

    @Test
    fun save_success_persistsNameAndAbout_andSetsConfirmation() {
        coEvery { profileRepository.updateProfile("Mary B", "Hi there") } returns
            AppResult.Success(User("u1", "mary", "Mary B", about = "Hi there"))
        val vm = viewModel()

        vm.onDisplayNameChange("Mary B")
        vm.onAboutChange("Hi there")
        vm.save()

        assertEquals("Mary B", vm.state.value.displayName)
        assertEquals("Hi there", vm.state.value.about)
        assertTrue(vm.state.value.savedConfirmation)
        coVerify { profileRepository.updateProfile("Mary B", "Hi there") }
    }

    @Test
    fun onToggleLastSeen_persistsSetting() {
        coEvery { profileRepository.setShowLastSeen(false) } returns
            AppResult.Success(User("u1", "mary", "Mary", showLastSeen = false))
        val vm = viewModel()

        vm.onToggleLastSeen(false)

        assertEquals(false, vm.state.value.showLastSeen)
        coVerify { profileRepository.setShowLastSeen(false) }
    }

    @Test
    fun confirmLogout_logsOut_andEmitsLoggedOut() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.confirmLogout()
            assertEquals(ProfileEffect.LoggedOut, awaitItem())
        }
        coVerify { authRepository.logout() }
    }
}
