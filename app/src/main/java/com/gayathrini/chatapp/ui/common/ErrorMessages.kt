package com.gayathrini.chatapp.ui.common

import com.gayathrini.chatapp.core.common.AppError

/**
 * Plain-language, reassuring error copy for elderly users (TDD §5.1, §8.4). Feature-specific
 * variants override the generic text where a friendlier phrasing helps.
 */
fun AppError.toUserMessage(): String = when (this) {
    AppError.Network -> "We couldn't connect. Please check your internet connection and try again."
    AppError.Unauthorized -> "Your session has expired. Please log in again."
    is AppError.Validation -> message ?: "Please check the information you entered and try again."
    is AppError.Conflict -> message ?: "That didn't work. Please try again."
    AppError.NotFound -> "We couldn't find what you were looking for."
    AppError.RateLimited -> "Too many attempts. Please wait a moment and try again."
    AppError.Server -> "Something went wrong on our side. Please try again shortly."
    is AppError.Unknown -> "Something went wrong. Please try again."
}

/** Login treats `401` as bad credentials rather than an expired session. */
fun AppError.toLoginMessage(): String = when (this) {
    AppError.Unauthorized -> "That username or password was not correct. Please try again."
    else -> toUserMessage()
}

/** Register gives a clearer message for a duplicate username. */
fun AppError.toRegisterMessage(): String = when (this) {
    is AppError.Conflict -> "That username is already taken. Please choose another."
    else -> toUserMessage()
}
