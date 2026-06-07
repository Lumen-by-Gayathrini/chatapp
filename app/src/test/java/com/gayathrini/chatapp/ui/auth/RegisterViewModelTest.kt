package com.gayathrini.chatapp.ui.auth

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.auth.AuthRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun fillValid() {
        viewModel.onDisplayNameChange("Mary")
        viewModel.onUsernameChange("mary")
        viewModel.onPasswordChange("secret1")
        viewModel.onConfirmPasswordChange("secret1")
    }

    @Test
    fun submit_blankFields_showsFieldErrors_andDoesNotCallRepo() {
        viewModel.submit()
        assertEquals("Please enter your name.", viewModel.state.value.displayNameError)
        assertEquals("Please choose a username.", viewModel.state.value.usernameError)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any()) }
    }

    @Test
    fun submit_passwordMismatch_showsError() {
        viewModel.onDisplayNameChange("Mary")
        viewModel.onUsernameChange("mary")
        viewModel.onPasswordChange("secret1")
        viewModel.onConfirmPasswordChange("different")
        viewModel.submit()
        assertEquals("The passwords do not match.", viewModel.state.value.confirmPasswordError)
    }

    @Test
    fun submit_success_emitsNavigateHome() = runTest {
        coEvery { authRepository.register("mary", "secret1", "Mary") } returns
            AppResult.Success(User("u1", "mary", "Mary"))
        fillValid()
        viewModel.effects.test {
            viewModel.submit()
            assertEquals(RegisterEffect.NavigateHome, awaitItem())
        }
    }

    @Test
    fun submit_conflict_showsUsernameTakenMessage() {
        coEvery { authRepository.register(any(), any(), any()) } returns
            AppResult.Failure(AppError.Conflict())
        fillValid()
        viewModel.submit()
        assertEquals(
            "That username is already taken. Please choose another.",
            viewModel.state.value.formError,
        )
    }
}
