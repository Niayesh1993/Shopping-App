package com.zozi.shoppingapp.domain.result

import com.zozi.shoppingapp.domain.error.AppError

sealed interface Outcome<out T> {
    data class Success<out T>(val data: T) : Outcome<T>

    data class Failure(val error: AppError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is Outcome.Success -> onSuccess(data)
    is Outcome.Failure -> onFailure(error)
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.data

fun <T> Outcome<T>.errorOrNull(): AppError? = (this as? Outcome.Failure)?.error
