package com.zozi.shoppingapp.presentation.search

import app.cash.turbine.test
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.Product
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ProductRepository = mockk()

    private val viewModel by lazy { SearchViewModel(SearchProductsUseCase(repository)) }

    private fun runViewModelTest(body: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.testDispatcher, testBody = body)

    @Before
    fun stubDefaultResponse() {
        respondWith(ProductPage.EMPTY)
    }

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

    private fun assertNoSearchFor(query: String) =
        coVerify(exactly = 0) { repository.searchProducts(query = query, page = any()) }

    /** The catalogue is the query-less request the screen makes before the user types. */
    private fun assertCatalogueBrowsed(times: Int = 1) =
        coVerify(exactly = times) { repository.searchProducts(query = null, page = 1) }

    /**
     * Creating the ViewModel immediately browses the catalogue. Most tests are about what happens
     * after that, so they let it settle first and then re-stub for the query under test.
     */
    private fun TestScope.givenTheCatalogueHasLoaded(
        products: List<Product> = listOf(product(id = 99)),
    ) {
        respondWith(productPage(products = products))
        viewModel
        advanceUntilIdle()
    }

    @Test
    fun `given a new view model when it is created then the catalogue is browsed with no query`() = runViewModelTest {
        // Given
        respondWith(productPage(products = listOf(product(id = 1), product(id = 2))))

        // When the ViewModel is created and nothing is typed
        viewModel
        advanceUntilIdle()

        // Then there are products to scroll before the user has searched for anything
        val state = viewModel.uiState.value
        assertEquals(listOf(1L, 2L), state.products.map { it.id })
        assertEquals("", state.query)
        assertFalse(state.isIdle)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertCatalogueBrowsed()
    }

    @Test
    fun `given an endpoint that rejects a query-less request when the app opens then it prompts to search`() = runViewModelTest {
        // Given an endpoint that only searches and cannot list the catalogue
        respondWith(AppError.Client(400))

        // When
        viewModel
        advanceUntilIdle()

        // Then the user is asked for a search term rather than shown an error they cannot act on
        val state = viewModel.uiState.value
        assertTrue(state.isIdle)
        assertNull(state.error)
    }

    @Test
    fun `given the network is down when the app opens then it prompts to search instead of erroring`() = runViewModelTest {
        // Given
        respondWith(AppError.NoConnection)

        // When
        viewModel
        advanceUntilIdle()

        // Then the app never opens on an error for a request the user did not make
        val state = viewModel.uiState.value
        assertTrue(state.isIdle)
        assertNull(state.error)
    }

    @Test
    fun `given a transient failure when the query is cleared then browsing is tried again`() = runViewModelTest {
        // Given a first browse that failed for a reason that may not last
        respondWith(AppError.NoConnection)
        viewModel
        advanceUntilIdle()

        // When the user searches and then clears the field
        respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        viewModel.onQueryChange("")
        advanceUntilIdle()

        // Then the catalogue is attempted again, unlike an outright rejection
        assertCatalogueBrowsed(times = 2)
        assertEquals(listOf(1L), viewModel.uiState.value.products.map { it.id })
    }

    @Test
    fun `given a query typed while the catalogue is loading when the catalogue fails then the query survives`() = runViewModelTest {
        // Given a browse that is still in flight
        respondWith(AppError.Timeout)

        // When the user types before it comes back
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // Then the late failure must not wipe what they typed, or retry would lose the query
        assertEquals("apple", viewModel.uiState.value.query)
    }

    @Test
    fun `given browsing was rejected once when the query is cleared then it is not retried`() = runViewModelTest {
        // Given a first browse the endpoint refused
        respondWith(AppError.Client(400))
        viewModel
        advanceUntilIdle()
        assertCatalogueBrowsed(times = 1)

        // When the user searches and then clears the field
        respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        viewModel.onQueryChange("")
        advanceUntilIdle()

        // Then the endpoint is not asked again for something it already refused
        assertCatalogueBrowsed(times = 1)
        assertTrue(viewModel.uiState.value.isIdle)
    }

    @Test
    fun `given a typed query when it is reported then the state shows it immediately`() = runViewModelTest {
        // Given a freshly created ViewModel

        // When
        viewModel.onQueryChange("app")

        // Then
        assertEquals("app", viewModel.uiState.value.query)
    }

    @Test
    fun `given a typed query when the debounce window has not elapsed then no search happens`() = runViewModelTest {
        // Given
        givenTheCatalogueHasLoaded()
        viewModel.onQueryChange("apple")

        // When
        advanceTimeBy(QUERY_DEBOUNCE_MILLIS - 1)

        // Then
        assertNoSearchFor("apple")
    }

    @Test
    fun `given a typed query when the debounce window elapses then the query is searched once`() = runViewModelTest {
        // Given
        givenTheCatalogueHasLoaded()
        respondWith(productPage(products = listOf(product(id = 1))))

        // When
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
        assertCatalogueBrowsed()
        confirmVerified(repository)
    }

    @Test
    fun `given rapid keystrokes when the debounce elapses then only the last query is searched`() = runViewModelTest {
        // Given
        givenTheCatalogueHasLoaded()
        respondWith(productPage())

        // When
        viewModel.onQueryChange("a")
        advanceTimeBy(50)
        viewModel.onQueryChange("ap")
        advanceTimeBy(50)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
        assertCatalogueBrowsed()
        confirmVerified(repository)
    }

    @Test
    fun `given a search in flight when it completes then loading is published before the content`() = runViewModelTest {
        // Given the catalogue has already settled, so the only state left to observe is the search
        givenTheCatalogueHasLoaded()
        respondWith(
            productPage(
                products = listOf(product(id = 1), product(id = 2)),
                currentPage = 1,
                pageCount = 3,
                totalResults = 70,
            ),
        )

        viewModel.uiState.test {
            assertEquals("", awaitItem().query)

            // When
            viewModel.onQueryChange("apple")
            assertEquals("apple", awaitItem().query)
            advanceTimeBy(QUERY_DEBOUNCE_MILLIS + 1)

            // Then
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

        // Then
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
        assertEquals(AppError.NoConnection, state.fullScreenError)
        assertFalse(state.isLoading)
        assertTrue(state.products.isEmpty())
    }

    @Test
    fun `given a failed search when retry is tapped then the same query runs again and recovers`() = runViewModelTest {
        // Given
        respondWith(AppError.Timeout)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        assertEquals(AppError.Timeout, viewModel.uiState.value.error)

        // When
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
    fun `given results on screen when the query is cleared then the catalogue comes back`() = runViewModelTest {
        // Given
        givenTheCatalogueHasLoaded()
        respondWith(productPage(products = listOf(product(id = 1))))
        viewModel.onQueryChange("apple")
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.products.map { it.id })

        // When
        respondWith(productPage(products = listOf(product(id = 99))))
        viewModel.onQueryChange("")
        advanceUntilIdle()

        // Then the user is left browsing again rather than staring at an empty screen
        val state = viewModel.uiState.value
        assertFalse(state.isIdle)
        assertEquals(listOf(99L), state.products.map { it.id })
        assertCatalogueBrowsed(times = 2)
    }

    @Test
    fun `given a whitespace only query when the debounce elapses then the catalogue is browsed`() = runViewModelTest {
        // Given
        givenTheCatalogueHasLoaded()

        // When
        viewModel.onQueryChange("   ")
        advanceUntilIdle()

        // Then whitespace is treated as "no query", not as a search for spaces
        assertNull(viewModel.uiState.value.error)
        assertNoSearchFor("   ")
        assertCatalogueBrowsed(times = 2)
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

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(1L), state.products.map { it.id })
        assertEquals(AppError.Timeout, state.nextPageError)
        assertNull(state.error)
        assertNull(state.fullScreenError)
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

        // Then
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
