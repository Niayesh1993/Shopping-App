package com.zozi.shoppingapp.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.presentation.search.SearchTestTags

@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(color = iconBackground, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 320.dp),
            )
        }

        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .testTag(SearchTestTags.ERROR_RETRY),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag(SearchTestTags.SECONDARY_ACTION),
            ) {
                Text(text = secondaryActionLabel)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MessageStateIdlePreview() {
    PreviewContainer {
        MessageState(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.search_idle_title),
            subtitle = stringResource(R.string.search_idle_subtitle),
        )
    }
}

@PreviewLightDark
@Composable
private fun MessageStateEmptyPreview() {
    PreviewContainer {
        MessageState(
            icon = Icons.Filled.SearchOff,
            title = stringResource(R.string.search_empty_title),
            subtitle = stringResource(R.string.search_empty_subtitle, "zzzzz"),
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconBackground = MaterialTheme.colorScheme.surfaceVariant,
            secondaryActionLabel = stringResource(R.string.search_empty_action),
            onSecondaryAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MessageStateErrorPreview() {
    PreviewContainer {
        MessageState(
            icon = Icons.Filled.CloudOff,
            title = stringResource(R.string.error_no_connection_title),
            subtitle = stringResource(R.string.error_no_connection),
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.errorContainer,
            actionLabel = stringResource(R.string.action_retry),
            onAction = {},
        )
    }
}

/** A non-retryable error: same layout, no action button. */
@Preview(name = "Error without retry")
@Composable
private fun MessageStateErrorWithoutRetryPreview() {
    PreviewContainer {
        MessageState(
            icon = Icons.Filled.CloudOff,
            title = stringResource(R.string.error_no_connection_title),
            subtitle = stringResource(R.string.error_no_connection),
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.errorContainer,
        )
    }
}
