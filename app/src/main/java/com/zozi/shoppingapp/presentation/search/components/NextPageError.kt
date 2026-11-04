package com.zozi.shoppingapp.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zozi.shoppingapp.presentation.search.SearchTestTags

/**
 * Footer shown when loading a further page failed. The products already on screen stay put, so
 * this stays inline and offers a retry rather than replacing the list with an error screen.
 */
@Composable
fun NextPageError(
    message: String,
    actionLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(SearchTestTags.NEXT_PAGE_ERROR),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(
                onClick = onRetry,
                modifier = Modifier.testTag(SearchTestTags.NEXT_PAGE_RETRY),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
