package com.gayathrini.chatapp.core.common

/**
 * App-wide error taxonomy (TDD §9). Repositories map transport/HTTP failures into one of these so
 * the UI layer can render plain-language messages without knowing about HTTP or Retrofit.
 */
sealed interface AppError {
    /** No connectivity / timeout / IO failure. */
    data object Network : AppError

    /** 401 — the session is invalid and a refresh did not recover it. */
    data object Unauthorized : AppError

    /** 400/422 — per-field validation errors, keyed by field name. */
    data class Validation(val fields: Map<String, String> = emptyMap(), val message: String? = null) : AppError

    /** 409 — conflict (e.g. duplicate). */
    data class Conflict(val message: String? = null) : AppError

    /** 404 — not found. */
    data object NotFound : AppError

    /** 429 — rate limited. */
    data object RateLimited : AppError

    /** 5xx — server error. */
    data object Server : AppError

    /** Anything else. */
    data class Unknown(val cause: Throwable? = null) : AppError
}
