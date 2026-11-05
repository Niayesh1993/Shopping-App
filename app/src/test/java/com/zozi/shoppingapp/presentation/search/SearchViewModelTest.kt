package com.zozi.shoppingapp.presentation.search

import app.cash.turbine.test
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import com.zozi.shoppingapp.domain.usecase.SearchProductsUseCase
import com.zozi.shoppingapp.presentation.search.SearchViewModel.Companion.QUERY_DEBOUNCE_MILLIS
import com.zozi.shoppingapp.testing.MainDispatcherRule
import com.zozi.shoppingapp.testing.product
import com.zozi.shoppingapp.testing.productPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ProductRepository = mockk()

    /**
     * Built lazily so the test dispatcher is already installed when `viewModelScope` is created:
     * the `init` block starts collecting the debounced query stream as soon as this returns.
     */
    private val viewModel by lazy { SearchViewModel(SearchProductsUseCase(repository)) }

    /** Shares one scheduler between [runTest] and `viewModelScope`, so time control applies to both. */
    private fun runViewModelTest(body: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.testDispatcher, testBody = body)

    /**
     * A harmless answer for calls no test planned for. Tests record their own stub on top, which
     * wins, but this one has to exist: [runTest] drains the scheduler once the body returns, so a
     * query typed by a test that never expects a search still reaches the repository on the way out.
     */
    @Before
    fun stubDefaultResponse() {
        respondWith(ProductPage.EMPTY)
    }

    /**
     * Answers every page with [outcome].
     *
     * The [yield] stands in for the suspension a network-backed repository always has. Without it
     * the call returns within the same dispatch that set `isLoading`, and `StateFlow` conflates the
     * loading value away before any collector can observe it.
     */
    private fun respondWith(outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), any()) } coAnswers {
            yield()
            outcome
        }
    }

    private fun respondWith(page: ProductPage) = respondWith(Outcome.Success(page))

    private fun respondWith(error: AppError) = respondWith(Outcome.Failure(error))

    /** Overrides [respondWith] for a single page, so pagination can succeed or fail on its own. */
    private fun respondToPage(page: Int, outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), page) } coAnswers {
            yield()
            outcome
        }
    }

    private fun assertNoSearches() =
        coVerify(exactly = 0) { repository.searchProducts(any(), any()) }

    @Test
    fun `given a new view model when nothing has been typed then the state is idle`() = runViewModelTest {
        // Given a freshly created ViewModel

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isIdle)
        assertEquals("", state.query)
        assertTrue(state.products.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `given a typed query when it is reported then the state shows it immediately`() = runViewModelTest {
        // Given a freshly created ViewModel

        // When
        viewModel.onQueryChange("app")

        // Then the field echoes the keystroke without waiting for the debounce
        assertEquals("app", viewModel.uiState.value.query)
    }

    @Test
    fun `given a typed query when the debounce window has not elapsed then no search happens`() = runViewModelTest {
        // Given
        viewModel.onQueryChange("apple")

        // When
        advanceTimeBy(QUERY_DEBOUNCE_MILLIS - 1)

        // Then
        assertNoSearches()
    }

    @Test
    fun `given a typed query when the debounce window elapses then the query is searched once`() = runViewModelTest {
        // Given
        respondWith(productPage(products = listOf(product(id = 1))))

        // When
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
        confirmVerified(repository)
    }

    @Test
    fun `given rapid keystrokes when the debounce elapses then only the last query is searched`() = runViewModelTest {
        // Given
        respondWith(productPage())

        // When
        viewModel.onQueryChange("a")
        advanceTimeBy(50)
        viewModel.onQueryChange("ap")
        advanceTimeBy(50)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // Then `confirmVerified` fails the test if "a" or "ap" also reached the repository
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
        confirmVerified(repository)
    }

    @Test
    fun `given a search in flight when it completes then loading is published before the content`() = runViewModelTest {
        // Given
        respondWith(
            productPage(
                products = listOf(product(id = 1), product(id = 2)),
                currentPage = 1,
                pageCount = 3,
                totalResults = 70,
            ),
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().isIdle)

            // When
            viewModel.onQueryChange("apple")
            assertEquals("apple", awaitItem().query)
            advanceTimeBy(QUERY_DEBOUNCE_MILLIS + 1)

            // Then the spinner is a state the UI really passes through, not an inferred one
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
    fun `given a query with no matches when the search completes then the state is an empty result`() = runViewModelTest {
        // Given
        respondWith(productPage(products = emptyList(), pageCount = 0, totalResults = 0))

        // When
        viewModel.onQueryChange("zzzzz")
        advanceUntilIdle()

        // Then "found nothing" is a different state from "has not searched yet"
        val state = viewModel.uiState.value
        assertTrue(state.isEmptyResult)
        assertFalse(state.isIdle)
        assertNull(state.error)
    }

    @Test
    fun `given the first page fails when the search completes then a full screen error is shown`() = runViewModelTest {
        // Given
        respondWith(AppError.NoConnection)

        // When
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(AppError.NoConnection, state.error)
        assertTrue(state.showsFullScreenError)
        assertFalse(state.isLoading)
        assertTrue(state.products.isEmpty())
    }

    @Test
    fun `given a failed search when retry is tapped then the same query runs again and recovers`() = runViewModelTest {
        // Given a search that has already failed
        respondWith(AppError.Timeout)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        assertEquals(AppError.Timeout, viewModel.uiState.value.error)

        // When the repository starts answering and the user retries.
        // Re-stubbing the same call replaces the failure: the newest answer wins.
        respondWith(productPage(products = listOf(product(id = 7))))
        viewModel.onRetry()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(listOf(7L), state.products.map { it.id })
        coVerify(exactly = 2) { repository.searchProducts(query = "apple", page = 1) }
    }

    @Test
    fun `given results on screen when the query is cleared then the state returns to idle`() = runViewModelTest {
        // Given
        respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isIdle)

        // When
        viewModel.onQueryChange("")
        advanceUntilIdle()

        // Then clearing shows the prompt again and does not search for nothing
        val state = viewModel.uiState.value
        assertTrue(state.isIdle)
        assertTrue(state.products.isEmpty())
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
        confirmVerified(repository)
    }

    @Test
    fun `given a whitespace only query when the debounce elapses then it stays idle without an error`() = runViewModelTest {
        // Given a freshly created ViewModel

        // When
        viewModel.onQueryChange("   ")
        advanceUntilIdle()

        // Then "nothing to search for" is the idle prompt, not a failure to read
        val state = viewModel.uiState.value
        assertTrue(state.isIdle)
        assertNull(state.error)
        assertNoSearches()
    }

    @Test
    fun `given a padded query when the debounce elapses then the repository receives it trimmed`() = runViewModelTest {
        // Given
        respondWith(productPage(products = listOf(product(id = 1))))

        // When
        viewModel.onQueryChange("  apple  ")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
    }

    @Test
    fun `given a first page with more to come when the next page loads then it is appended`() = runViewModelTest {
        // Given
        respondToPage(
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
        respondToPage(
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

        // When
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(1L, 2L), state.products.map { it.id })
        assertEquals(2, state.currentPage)
        assertFalse(state.hasNextPage)
        assertFalse(state.isLoadingNextPage)
    }

    @Test
    fun `given the last page is loaded when pagination is requested then it is ignored`() = runViewModelTest {
        // Given
        respondWith(
            productPage(products = listOf(product(id = 1)), currentPage = 1, pageCount = 1),
        )
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // When
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.searchProducts(query = any(), page = 2) }
    }

    @Test
    fun `given results on screen when the next page fails then they stay and the error is inline`() = runViewModelTest {
        // Given
        respondToPage(
            1,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 1)),
                    currentPage = 1,
                    pageCount = 2,
                ),
            ),
        )
        respondToPage(2, Outcome.Failure(AppError.Timeout))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // When
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        // Then a pagination failure must not throw away what the user is already reading
        val state = viewModel.uiState.value
        assertEquals(listOf(1L), state.products.map { it.id })
        assertEquals(AppError.Timeout, state.nextPageError)
        assertNull(state.error)
        assertFalse(state.showsFullScreenError)
    }

    @Test
    fun `given a product repeated across pages when the next page loads then it appears once`() = runViewModelTest {
        // Given
        respondToPage(
            1,
            Outcome.Success(
                productPage(
                    products = listOf(product(id = 1), product(id = 2)),
                    currentPage = 1,
                    pageCount = 2,
                ),
            ),
        )
        respondToPage(
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

        // When
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        // Then the id shared by both pages is not rendered twice
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.products.map { it.id })
    }

    @Test
    fun `given results for one query when another is searched then the old results are replaced`() = runViewModelTest {
        // Given
        respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // When
        respondWith(productPage(products = listOf(product(id = 9))))
        viewModel.onQueryChange("watch")
        advanceUntilIdle()

        // Then
        assertEquals(listOf(9L), viewModel.uiState.value.products.map { it.id })
        coVerifyOrder {
            repository.searchProducts(query = "apple", page = 1)
            repository.searchProducts(query = "watch", page = 1)
        }
    }
}
