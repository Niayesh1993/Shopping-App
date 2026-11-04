package com.zozi.shoppingapp.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.error.AppError

@Composable
fun AppError.asMessage(): String = when (this) {
    AppError.NoConnection -> stringResource(R.string.error_no_connection)
    AppError.Timeout -> stringResource(R.string.error_timeout)
    AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
    AppError.NotFound -> stringResource(R.string.error_not_found)
    AppError.Serialization -> stringResource(R.string.error_serialization)
    AppError.EmptyQuery -> stringResource(R.string.error_empty_query)
    is AppError.Server -> stringResource(R.string.error_server, code)
    is AppError.Client -> stringResource(R.string.error_client, code)
    is AppError.Unknown -> stringResource(R.string.error_unknown)
}

val AppError.isRetryable: Boolean
    get() = when (this) {
        AppError.NoConnection,
        AppError.Timeout,
        AppError.Serialization,
        is AppError.Server,
        is AppError.Unknown,
        -> true

        AppError.Unauthorized,
        AppError.NotFound,
        AppError.EmptyQuery,
        is AppError.Client,
        -> false
    }
