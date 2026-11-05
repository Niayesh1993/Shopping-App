package com.zozi.shoppingapp.data.remote

import com.zozi.shoppingapp.domain.error.AppError
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {

    @Test
    fun `given a socket timeout when mapping it then it becomes Timeout`() {
        // Given
        val thrown = SocketTimeoutException()

        // When
        val error = thrown.toAppError()

        // Then
        assertEquals(AppError.Timeout, error)
    }

    @Test
    fun `given a connectivity failure when mapping it then it becomes NoConnection`() {
        // Given / When / Then: every way the device can fail to reach the host reads the same
        assertEquals(AppError.NoConnection, UnknownHostException().toAppError())
        assertEquals(AppError.NoConnection, ConnectException().toAppError())
        assertEquals(AppError.NoConnection, IOException("socket closed").toAppError())
    }

    @Test
    fun `given a 401 or 403 response when mapping it then it becomes Unauthorized`() {
        // Given / When / Then
        assertEquals(AppError.Unauthorized, httpException(401).toAppError())
        assertEquals(AppError.Unauthorized, httpException(403).toAppError())
    }

    @Test
    fun `given a 404 response when mapping it then it becomes NotFound`() {
        // Given
        val thrown = httpException(404)

        // When
        val error = thrown.toAppError()

        // Then
        assertEquals(AppError.NotFound, error)
    }

    @Test
    fun `given a 5xx response when mapping it then it becomes Server with the status code`() {
        // Given / When / Then: the code is kept so the UI can show which failure it was
        assertEquals(AppError.Server(500), httpException(500).toAppError())
        assertEquals(AppError.Server(503), httpException(503).toAppError())
    }

    @Test
    fun `given another 4xx response when mapping it then it becomes Client with the status code`() {
        // Given / When / Then
        assertEquals(AppError.Client(400), httpException(400).toAppError())
        assertEquals(AppError.Client(429), httpException(429).toAppError())
    }

    @Test
    fun `given a malformed payload when mapping it then it becomes Serialization`() {
        // Given
        val thrown = SerializationException("bad json")

        // When
        val error = thrown.toAppError()

        // Then
        assertEquals(AppError.Serialization, error)
    }

    @Test
    fun `given an unrecognised exception when mapping it then it becomes Unknown with the message`() {
        // Given
        val thrown = IllegalStateException("boom")

        // When
        val error = thrown.toAppError()

        // Then
        assertEquals(AppError.Unknown("boom"), error)
    }

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType())),
    )
}
