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
    /**
     * Nothing to show and nothing to blame: the catalogue could not be browsed, so the screen asks
     * for a search term instead. Normally the app opens on the catalogue and never reaches this.
     */
    val isIdle: Boolean
        get() = query.isBlank() && products.isEmpty() && !isLoading && error == null

    val isEmptyResult: Boolean
        get() = query.isNotBlank() && products.isEmpty() && !isLoading && error == null

    /** An error with no results to fall back on, so the whole screen becomes the error. */
    val fullScreenError: AppError?
        get() = error?.takeIf { products.isEmpty() }
}
