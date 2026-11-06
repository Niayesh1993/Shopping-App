package com.zozi.shoppingapp.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.Product
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.result.fold
import com.zozi.shoppingapp.domain.usecase.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchProducts: SearchProductsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Null until the user types, so the initial value is skipped by content rather than position. */
    private val queryInput = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null
    private var nextPageJob: Job? = null

    /**
     * Set to false once the endpoint has refused a query-less request outright, which means it only
     * searches and cannot browse, so there is nothing to gain by asking again.
     */
    private var canBrowseCatalogue = true

    @OptIn(FlowPreview::class)
    private val debouncedQueries = queryInput
        .filterNotNull()
        .debounce(QUERY_DEBOUNCE_MILLIS)
        .distinctUntilChanged()

    init {
        debouncedQueries
            .onEach(::search)
            .launchIn(viewModelScope)

        // Opening the app is itself a request for the catalogue, so there is nothing to debounce.
        search(query = "")
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryInput.value = query
    }

    fun onRetry() {
        search(_uiState.value.query)
    }

    fun onLoadNextPage() {
        val state = _uiState.value
        val canPaginate = state.hasNextPage &&
            !state.isLoading &&
            !state.isLoadingNextPage &&
            nextPageJob?.isActive != true
        if (!canPaginate) return

        nextPageJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNextPage = true, nextPageError = null) }
            searchProducts(state.query, state.currentPage + 1).fold(
                onSuccess = { page ->
                    _uiState.update { current ->
                        current.copy(
                            isLoadingNextPage = false,
                            products = (current.products + page.products)
                                .distinctBy(Product::id),
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                            totalResults = page.totalResults,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoadingNextPage = false, nextPageError = error)
                    }
                },
            )
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        nextPageJob?.cancel()

        if (query.isBlank() && !canBrowseCatalogue) {
            promptForSearchTerm(query)
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, nextPageError = null)
            }
            searchProducts(query, ProductPage.FIRST_PAGE).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = page.products,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                            totalResults = page.totalResults,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    // A query-less request is the screen's own idea, not the user's, so failing it
                    // must never open the app on an error they did not ask for. Fall back to
                    // prompting for a term; a real search will surface the error properly.
                    if (query.isBlank()) {
                        canBrowseCatalogue = !error.rejectsBrowsing
                        promptForSearchTerm(query)
                        return@fold
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = emptyList(),
                            totalResults = 0,
                            currentPage = 0,
                            hasNextPage = false,
                            error = error,
                        )
                    }
                },
            )
        }
    }

    /**
     * Clears the screen back to the "type a search term" prompt — but never over the top of a query
     * the user has typed in the meantime, since the catalogue request they did not ask for can
     * finish long after they started typing.
     */
    private fun promptForSearchTerm(query: String) {
        if (_uiState.value.query.isBlank()) {
            _uiState.value = SearchUiState(query = query)
        }
    }

    companion object {
        const val QUERY_DEBOUNCE_MILLIS = 350L
    }
}

/**
 * Whether the endpoint turned the request down as malformed, which means it cannot browse at all and
 * there is no point asking again. Everything else — no connection, a timeout, a missing API key —
 * may well succeed later, so those failures are not remembered.
 */
private val AppError.rejectsBrowsing: Boolean
    get() = this is AppError.Client || this == AppError.NotFound
