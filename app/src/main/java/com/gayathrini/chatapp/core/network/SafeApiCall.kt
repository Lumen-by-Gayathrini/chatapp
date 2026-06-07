package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.dto.ErrorEnvelope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs an API call off the main thread and maps transport/HTTP failures into [AppError] (TDD §9).
 * Repositories wrap every network call in this so the rest of the app never sees Retrofit/HTTP.
 */
suspend fun <T> safeApiCall(
    dispatchers: DispatcherProvider,
    json: Json,
    block: suspend () -> T,
): AppResult<T> = withContext(dispatchers.io) {
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.Failure(e.toAppError(json))
    } catch (e: IOException) {
        AppResult.Failure(AppError.Network)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown(e))
    }
}

/** Maps an HTTP error response into an [AppError], parsing the standard error envelope when present. */
fun HttpException.toAppError(json: Json): AppError {
    val envelope = runCatching {
        response()?.errorBody()?.string()?.let { json.decodeFromString<ErrorEnvelope>(it) }
    }.getOrNull()
    val message = envelope?.error?.message
    return when (code()) {
        400, 422 -> AppError.Validation(envelope?.error?.details ?: emptyMap(), message)
        401 -> AppError.Unauthorized
        404 -> AppError.NotFound
        409 -> AppError.Conflict(message)
        429 -> AppError.RateLimited
        in 500..599 -> AppError.Server
        else -> AppError.Unknown()
    }
}
