package com.zozi.shoppingapp.presentation.search

import app.cash.turbine.test
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.result.Outcome
import com.zozi.shoppingapp.domain.usecase.SearchProductsUseCase
import com.zozi.shoppingapp.presentation.search.SearchViewModel.Companion.QUERY_DEBOUNCE_MILLIS
import com.zozi.shoppingapp.testing.FakeProductRepository
import com.zozi.shoppingapp.testing.MainDispatcherRule
import com.zozi.shoppingapp.testing.product
import com.zozi.shoppingapp.testing.productPage
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()
    private val viewModel by lazy { SearchViewModel(SearchProductsUseCase(repository)) }

    /** Shares one scheduler between [runTest] and `viewModelScope`, so time control applies to both. */
    private fun runViewModelTest(body: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.testDispatcher, testBody = body)

    @Test
    fun `starts idle`() = runViewModelTest {
        val state = viewModel.uiState.value

        assertTrue(state.isIdle)
        assertEquals("", state.query)
        assertTrue(state.products.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `reflects the typed query immediately`() = runViewModelTest {
        viewModel.onQueryChange("app")

        assertEquals("app", viewModel.uiState.value.query)
    }

    @Test
    fun `does not search before the debounce window elapses`() = runViewModelTest {
        viewModel.onQueryChange("apple")
        advanceTimeBy(QUERY_DEBOUNCE_MILLIS - 1)

        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun `searches once the debounce window elapses`() = runViewModelTest {
        repository.respondWith(productPage(products = listOf(product(id = 1))))

        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        assertEquals(1, repository.requests.size)
        assertEquals("apple", repository.requests.single().query)
    }

    @Test
    fun `collapses rapid keystrokes into a single search`() = runViewModelTest {
        repository.respondWith(productPage())

        viewModel.onQueryChange("a")
        advanceTimeBy(50)
        viewModel.onQueryChange("ap")
        advanceTimeBy(50)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        assertEquals(listOf("apple"), repository.requests.map { it.query })
    }

    @Test
    fun `publishes loading then content`() = runViewModelTest {
        repository.respondWith(
            productPage(
                products = listOf(product(id = 1), product(id = 2)),
                currentPage = 1,
                pageCount = 3,
                totalResults = 70,
            ),
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().isIdle)

            viewModel.onQueryChange("apple")
            assertEquals("apple", awaitItem().query)

            advanceTimeBy(QUERY_DEBOUNCE_MILLIS + 1)
            assertTrue(awaitItem().isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf(1L, 2L), loaded.products.map { it.id })
            assertEquals(70, loaded.totalResults)
            assertTrue(loaded.hasNextPage)
            assertNull(loaded.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exposes an empty result for a query with no matches`() = runViewModelTest {
        repository.respondWith(productPage(products = emptyList(), pageCount = 0, totalResults = 0))

        viewModel.onQueryChange("zzzzz")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmptyResult)
        assertFalse(state.isIdle)
        assertNull(state.error)
    }

    @Test
    fun `surfaces a full screen error when the first page fails`() = runViewModelTest {
        repository.respondWith(AppError.NoConnection)

        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppError.NoConnection, state.error)
        assertTrue(state.showsFullScreenError)
        assertFalse(state.isLoading)
        assertTrue(state.products.isEmpty())
    }

    @Test
    fun `retry runs the current query again and recovers`() = runViewModelTest {
        repository.respondWith(AppError.Timeout)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        assertEquals(AppError.Timeout, viewModel.uiState.value.error)

        repository.respondWith(productPage(products = listOf(product(id = 7))))
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(listOf(7L), state.products.map { it.id })
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `clearing the query returns to idle and stops searching`() = runViewModelTest {
        repository.respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isIdle)

        viewModel.onQueryChange("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isIdle)
        assertTrue(state.products.isEmpty())
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `a whitespace only query stays idle instead of showing an error`() = runViewModelTest {
        viewModel.onQueryChange("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isIdle)
        assertNull(state.error)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun `trims the query before searching`() = runViewModelTest {
        repository.respondWith(productPage(products = listOf(product(id = 1))))

        viewModel.onQueryChange("  apple  ")
        advanceUntilIdle()

        assertEquals("apple", repository.requests.single().query)
    }

    @Test
    fun `appends the next page to the existing products`() = runViewModelTest {
        repository.respondToPage(
            1,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 1)),
                    currentPage = 1,
                    pageCount = 2,
                    totalResults = 2,
                ),
            ),
        )
        repository.respondToPage(
            2,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 2)),
                    currentPage = 2,
                    pageCount = 2,
                    totalResults = 2,
                ),
            ),
        )

        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        viewModel.onLoadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(1L, 2L), state.products.map { it.id })
        assertEquals(2, state.currentPage)
        assertFalse(state.hasNextPage)
        assertFalse(state.isLoadingNextPage)
    }

    @Test
    fun `ignores pagination when there is no next page`() = runViewModelTest {
        repository.respondWith(
            productPage(products = listOf(product(id = 1)), currentPage = 1, pageCount = 1),
        )
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        viewModel.onLoadNextPage()
        advanceUntilIdle()

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `keeps existing products when loading the next page fails`() = runViewModelTest {
        repository.respondToPage(
            1,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 1)),
                    currentPage = 1,
                    pageCount = 2,
                ),
            ),
        )
        repository.respondToPage(2, Outcome.Failure(AppError.Timeout))

        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(1L), state.products.map { it.id })
        assertEquals(AppError.Timeout, state.nextPageError)
        assertNull(state.error)
        assertFalse(state.showsFullScreenError)
    }

    @Test
    fun `does not add the same product twice across pages`() = runViewModelTest {
        repository.respondToPage(
            1,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 1), product(id = 2)),
                    currentPage = 1,
                    pageCount = 2,
                ),
            ),
        )
        repository.respondToPage(
            2,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 2), product(id = 3)),
                    currentPage = 2,
                    pageCount = 2,
                ),
            ),
        )

        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.products.map { it.id })
    }

    @Test
    fun `a new query replaces results from the previous one`() = runViewModelTest {
        repository.respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        repository.respondWith(productPage(products = listOf(product(id = 9))))
        viewModel.onQueryChange("watch")
        advanceUntilIdle()

        assertEquals(listOf(9L), viewModel.uiState.value.products.map { it.id })
        assertEquals(listOf("apple", "watch"), repository.requests.map { it.query })
    }
}
