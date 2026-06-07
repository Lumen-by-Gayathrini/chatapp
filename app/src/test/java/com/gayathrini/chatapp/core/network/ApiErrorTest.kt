package com.gayathrini.chatapp.core.network

import com.gayathrini.chatapp.core.common.AppError
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiErrorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun httpError(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun maps401ToUnauthorized() {
        val error = httpError(401, """{"error":{"code":"INVALID_CREDENTIALS","message":"no"}}""")
        assertEquals(AppError.Unauthorized, error.toAppError(json))
    }

    @Test
    fun maps409ToConflictWithMessage() {
        val error = httpError(409, """{"error":{"code":"CONFLICT","message":"duplicate"}}""")
        val mapped = error.toAppError(json)
        assertTrue(mapped is AppError.Conflict)
        assertEquals("duplicate", (mapped as AppError.Conflict).message)
    }

    @Test
    fun maps422ToValidationWithFields() {
        val error = httpError(
            422,
            """{"error":{"code":"VALIDATION_ERROR","message":"bad","details":{"username":"taken"}}}""",
        )
        val mapped = error.toAppError(json)
        assertTrue(mapped is AppError.Validation)
        assertEquals("taken", (mapped as AppError.Validation).fields["username"])
    }

    @Test
    fun maps500ToServer() {
        val error = httpError(500, """{"error":{"code":"INTERNAL","message":"x"}}""")
        assertEquals(AppError.Server, error.toAppError(json))
    }
}
