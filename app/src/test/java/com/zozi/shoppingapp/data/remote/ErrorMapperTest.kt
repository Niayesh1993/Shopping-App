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
    fun `maps timeouts`() {
        assertEquals(AppError.Timeout, SocketTimeoutException().toAppError())
    }

    @Test
    fun `maps connectivity failures`() {
        assertEquals(AppError.NoConnection, UnknownHostException().toAppError())
        assertEquals(AppError.NoConnection, ConnectException().toAppError())
        assertEquals(AppError.NoConnection, IOException("socket closed").toAppError())
    }

    @Test
    fun `maps authentication failures`() {
        assertEquals(AppError.Unauthorized, httpException(401).toAppError())
        assertEquals(AppError.Unauthorized, httpException(403).toAppError())
    }

    @Test
    fun `maps not found`() {
        assertEquals(AppError.NotFound, httpException(404).toAppError())
    }

    @Test
    fun `maps server failures with their status code`() {
        assertEquals(AppError.Server(500), httpException(500).toAppError())
        assertEquals(AppError.Server(503), httpException(503).toAppError())
    }

    @Test
    fun `maps remaining client failures with their status code`() {
        assertEquals(AppError.Client(400), httpException(400).toAppError())
        assertEquals(AppError.Client(429), httpException(429).toAppError())
    }

    @Test
    fun `maps malformed payloads`() {
        assertEquals(AppError.Serialization, SerializationException("bad json").toAppError())
    }

    @Test
    fun `maps anything else to unknown and keeps the message`() {
        assertEquals(AppError.Unknown("boom"), IllegalStateException("boom").toAppError())
    }

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType())),
    )
}
