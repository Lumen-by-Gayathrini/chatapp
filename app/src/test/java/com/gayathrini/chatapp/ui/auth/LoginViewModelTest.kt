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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submit_withBlankFields_showsFieldErrors_andDoesNotCallRepo() {
        viewModel.submit()

        assertEquals("Please enter your username.", viewModel.state.value.usernameError)
        assertEquals("Please enter your password.", viewModel.state.value.passwordError)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun submit_success_emitsNavigateHome() = runTest {
        coEvery { authRepository.login("mary", "pw") } returns AppResult.Success(User("u1", "mary", "Mary"))
        viewModel.onUsernameChange("mary")
        viewModel.onPasswordChange("pw")

        viewModel.effects.test {
            viewModel.submit()
            assertEquals(LoginEffect.NavigateHome, awaitItem())
        }
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun submit_failure_setsPlainLanguageFormError() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AppResult.Failure(AppError.Unauthorized)
        viewModel.onUsernameChange("mary")
        viewModel.onPasswordChange("wrong")

        viewModel.submit()

        assertEquals(
            "That username or password was not correct. Please try again.",
            viewModel.state.value.formError,
        )
    }
}
