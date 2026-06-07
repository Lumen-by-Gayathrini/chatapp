package com.gayathrini.chatapp.core.common

/**
 * A `Result`-style wrapper (TDD §9). Named [AppResult] to avoid clashing with [kotlin.Result].
 * Repositories return this; ViewModels [fold] it into UI state.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
