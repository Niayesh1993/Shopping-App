package com.zozi.shoppingapp.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.model.Product
import com.zozi.shoppingapp.presentation.search.components.MessageState
import com.zozi.shoppingapp.presentation.search.components.ProductCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::onRetry,
        onLoadNextPage = viewModel::onLoadNextPage,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.search_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (uiState.products.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.search_result_count,
                        uiState.totalResults,
                        uiState.totalResults,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag(SearchTestTags.RESULT_COUNT),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(SearchTestTags.LOADING),
                    )

                    uiState.showsFullScreenError -> ErrorState(
                        uiState = uiState,
                        onRetry = onRetry,
                    )

                    uiState.isIdle -> MessageState(
                        title = stringResource(R.string.search_idle_title),
                        subtitle = stringResource(R.string.search_idle_subtitle),
                        modifier = Modifier.testTag(SearchTestTags.IDLE),
                    )

                    uiState.isEmptyResult -> MessageState(
                        title = stringResource(R.string.search_empty_title),
                        subtitle = stringResource(R.string.search_empty_subtitle),
                        modifier = Modifier.testTag(SearchTestTags.EMPTY),
                    )

                    else -> ProductList(
                        products = uiState.products,
                        isLoadingNextPage = uiState.isLoadingNextPage,
                        onLoadNextPage = onLoadNextPage,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    uiState: SearchUiState,
    onRetry: () -> Unit,
) {
    val error = uiState.error ?: return
    MessageState(
        title = error.asMessage(),
        actionLabel = stringResource(R.string.action_retry).takeIf { error.isRetryable },
        onAction = onRetry.takeIf { error.isRetryable },
        modifier = Modifier.testTag(SearchTestTags.ERROR),
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(text = stringResource(R.string.search_hint)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.testTag(SearchTestTags.CLEAR_QUERY),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.testTag(SearchTestTags.SEARCH_FIELD),
    )
}

@Composable
private fun ProductList(
    products: List<Product>,
    isLoadingNextPage: Boolean,
    onLoadNextPage: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, products.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex >= products.lastIndex - PREFETCH_DISTANCE) {
                    onLoadNextPage()
                }
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.PRODUCT_LIST),
    ) {
        items(items = products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                modifier = Modifier.testTag(SearchTestTags.PRODUCT_CARD),
            )
        }

        if (isLoadingNextPage) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag(SearchTestTags.LOADING_NEXT_PAGE),
                    )
                }
            }
        }
    }
}

private const val PREFETCH_DISTANCE = 3
