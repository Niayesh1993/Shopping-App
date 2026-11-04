package com.zozi.shoppingapp.data.remote

import com.zozi.shoppingapp.domain.error.AppError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Translates transport/parsing failures into the error vocabulary the domain layer speaks. */
fun Throwable.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout
    is UnknownHostException, is ConnectException -> AppError.NoConnection
    is HttpException -> code().toHttpAppError()
    is SerializationException -> AppError.Serialization
    is IOException -> AppError.NoConnection
    else -> AppError.Unknown(message)
}

private fun Int.toHttpAppError(): AppError = when {
    this == 401 || this == 403 -> AppError.Unauthorized
    this == 404 -> AppError.NotFound
    this in 500..599 -> AppError.Server(this)
    else -> AppError.Client(this)
}
