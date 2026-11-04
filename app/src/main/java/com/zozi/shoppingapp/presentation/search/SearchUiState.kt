package com.zozi.shoppingapp.presentation.search

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.Product

data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val error: AppError? = null,
    val nextPageError: AppError? = null,
    val totalResults: Int = 0,
    val currentPage: Int = 0,
    val hasNextPage: Boolean = false,
) {
    val isIdle: Boolean
        get() = query.isBlank() && products.isEmpty() && !isLoading && error == null

    val isEmptyResult: Boolean
        get() = query.isNotBlank() && products.isEmpty() && !isLoading && error == null

    val showsFullScreenError: Boolean
        get() = error != null && products.isEmpty()
}
