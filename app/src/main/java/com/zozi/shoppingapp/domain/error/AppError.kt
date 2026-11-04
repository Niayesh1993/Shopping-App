package com.zozi.shoppingapp.domain.error

sealed interface AppError {
    data object NoConnection : AppError

    data object Timeout : AppError

    data object Unauthorized : AppError

    data object NotFound : AppError

    data class Server(val code: Int) : AppError

    data class Client(val code: Int) : AppError

    data object Serialization : AppError

    data object EmptyQuery : AppError

    data class Unknown(val message: String? = null) : AppError
}
