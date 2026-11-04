package com.zozi.shoppingapp.presentation.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.error.AppError

/** Short headline for the error screen. */
@Composable
fun AppError.asTitle(): String = when (this) {
    AppError.NoConnection -> stringResource(R.string.error_no_connection_title)
    AppError.Timeout -> stringResource(R.string.error_timeout_title)
    AppError.Unauthorized -> stringResource(R.string.error_unauthorized_title)
    AppError.NotFound -> stringResource(R.string.error_not_found_title)
    AppError.Serialization -> stringResource(R.string.error_serialization_title)
    AppError.EmptyQuery -> stringResource(R.string.error_empty_query_title)
    is AppError.Server -> stringResource(R.string.error_server_title)
    is AppError.Client -> stringResource(R.string.error_client_title)
    is AppError.Unknown -> stringResource(R.string.error_unknown_title)
}

/** The explanatory line under the headline, and the text used for the inline page error. */
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

val AppError.icon: ImageVector
    get() = when (this) {
        AppError.NoConnection -> Icons.Filled.CloudOff
        AppError.Timeout -> Icons.Filled.HourglassEmpty
        AppError.Unauthorized -> Icons.Filled.Lock
        AppError.NotFound -> Icons.Filled.SearchOff
        AppError.Serialization -> Icons.Filled.ReportProblem
        AppError.EmptyQuery -> Icons.Filled.Search
        is AppError.Server -> Icons.Filled.CloudOff
        is AppError.Client -> Icons.Filled.ErrorOutline
        is AppError.Unknown -> Icons.Filled.ErrorOutline
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
