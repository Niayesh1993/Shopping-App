package com.zozi.shoppingapp.presentation.search

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.zozi.shoppingapp.MainActivity
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import com.zozi.shoppingapp.testing.product
import com.zozi.shoppingapp.testing.productPage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * End-to-end through the real ViewModel, use case and DI graph, with only the network-backed
 * repository swapped out for a mock. Covers debounce, pagination and error recovery as the user
 * sees them.
 *
 * Test names read as given/when/then in `camelCase` rather than backticks, because `minSdk` is 24
 * and dex rejects method names containing spaces below API 30.
 */
@HiltAndroidTest
class SearchFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** The mock from `TestRepositoryModule`; `@Singleton` makes it the one the app uses. */
    @Inject
    lateinit var repository: ProductRepository

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /** Answers every page. Stubs recorded later win, so a test can re-stub to recover from failure. */
    private fun respondWith(outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), any()) } returns outcome
    }

    private fun respondWith(page: ProductPage) = respondWith(Outcome.Success(page))

    private fun respondWith(error: AppError) = respondWith(Outcome.Failure(error))

    private fun respondToPage(page: Int, outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), page) } returns outcome
    }

    private fun typeQuery(query: String) {
        composeRule.onNodeWithTag(SearchTestTags.SEARCH_FIELD).performTextReplacement(query)
    }

    private fun awaitProductList() {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.PRODUCT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Waits for [page] to have been requested at least [times].
     *
     * A mock has no request log to poll, so the verification itself is the predicate: it is retried
     * until it stops throwing. Going through [androidx.compose.ui.test.junit4.ComposeTestRule.waitUntil]
     * rather than MockK's own `timeout` matters, because the request is triggered by a scroll and
     * only `waitUntil` keeps driving the Compose clock while we wait.
     */
    private fun awaitRequestFor(page: Int, times: Int = 1) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            runCatching {
                coVerify(atLeast = times) { repository.searchProducts(any(), page) }
            }.isSuccess
        }
    }

    @Test
    fun givenAFreshLaunch_whenNothingIsTyped_thenTheIdlePromptShowsAndNothingIsSearched() {
        // Given / When the app has just started

        // Then
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertIsDisplayed()
        coVerify(exactly = 0) { repository.searchProducts(any(), any()) }
    }

    @Test
    fun givenTheRepositoryReturnsOnePage_whenAQueryIsTyped_thenTheMatchingProductsAppear() {
        // Given a single page, so the list does not immediately prefetch a second one
        respondWith(
            productPage(
                products = listOf(
                    product(id = 1, name = "Apple iPhone X 256GB Zilver"),
                    product(id = 2, name = "Apple TV 4K 64GB"),
                ),
                currentPage = 1,
                pageCount = 1,
                totalResults = 2,
            ),
        )

        // When
        typeQuery("apple")
        awaitProductList()

        // Then
        composeRule.onAllNodesWithTag(SearchTestTags.PRODUCT_CARD).assertCountEquals(2)
        composeRule.onNodeWithText("Apple iPhone X 256GB Zilver").assertIsDisplayed()
        composeRule.onNodeWithText("2 products").assertIsDisplayed()
        coVerify(exactly = 1) { repository.searchProducts(query = "apple", page = 1) }
    }

    @Test
    fun givenTheRepositoryFindsNothing_whenAQueryIsTyped_thenTheEmptyStateAppears() {
        // Given
        respondWith(productPage(products = emptyList(), pageCount = 0, totalResults = 0))

        // When
        typeQuery("zzzzz")

        // Then
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.EMPTY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.search_empty_title)).assertIsDisplayed()
    }

    @Test
    fun givenTheSearchFailed_whenRetryIsTapped_thenTheResultsAppear() {
        // Given a search that has failed and is showing its error
        respondWith(AppError.NoConnection)
        typeQuery("apple")
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.ERROR_RETRY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.error_no_connection)).assertIsDisplayed()

        // When the repository starts answering and the user retries
        respondWith(productPage(products = listOf(product(id = 7, name = "Apple Watch"))))
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).performClick()
        awaitProductList()

        // Then
        composeRule.onNodeWithText("Apple Watch").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertDoesNotExist()
        coVerify(exactly = 2) { repository.searchProducts(query = "apple", page = 1) }
    }

    @Test
    fun givenTwoPagesOfResults_whenScrollingToTheEnd_thenTheNextPageIsLoaded() {
        // Given
        respondToPage(
            1,
            Outcome.Success(
                productPage(
                    products = (1..20L).map { product(id = it, name = "Product $it") },
                    currentPage = 1,
                    pageCount = 2,
                    totalResults = 40,
                ),
            ),
        )
        respondToPage(
            2,
            Outcome.Success(
                productPage(
                    products = (21..40L).map { product(id = it, name = "Product $it") },
                    currentPage = 2,
                    pageCount = 2,
                    totalResults = 40,
                ),
            ),
        )

        typeQuery("product")
        awaitProductList()

        // When the last card of the first page is reached
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(19)

        // Then
        awaitRequestFor(page = 2)

        // The appended items exist but are off-screen: only visible rows are composed.
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(39)
        composeRule.onNodeWithText("Product 40").assertIsDisplayed()
    }

    @Test
    fun givenTheNextPageFails_whenTheInlineRetryIsTapped_thenTheResultsSurviveAndTheRestArrive() {
        // Given a first page that loads and a second page that fails
        val firstPage = productPage(
            products = (1..20L).map { product(id = it, name = "Product $it") },
            currentPage = 1,
            pageCount = 2,
            totalResults = 40,
        )
        respondToPage(1, Outcome.Success(firstPage))
        respondToPage(2, Outcome.Failure(AppError.Timeout))

        typeQuery("product")
        awaitProductList()

        // Only a handful of cards fit on screen, so the next page is requested on reaching the end.
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(19)
        awaitRequestFor(page = 2)
        composeRule.waitForIdle()

        // The failure footer is appended below the last card, so it starts just out of view.
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(FOOTER_INDEX)
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.NEXT_PAGE_RETRY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The first page stays on screen rather than being replaced by an error screen.
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertDoesNotExist()

        // When the repository starts answering and the user retries
        respondToPage(
            2,
            Outcome.Success(
                productPage(
                    products = (21..40L).map { product(id = it, name = "Product $it") },
                    currentPage = 2,
                    pageCount = 2,
                    totalResults = 40,
                ),
            ),
        )
        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_RETRY).performClick()

        // Then
        awaitRequestFor(page = 2, times = 2)
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(39)
        composeRule.onNodeWithText("Product 40").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_ERROR).assertDoesNotExist()
    }

    @Test
    fun givenResultsOnScreen_whenTheQueryIsCleared_thenTheIdlePromptReturns() {
        // Given results on screen
        respondWith(productPage(products = listOf(product(id = 1))))

        typeQuery("apple")
        awaitProductList()

        // When
        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).performClick()

        // Then
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.IDLE)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertDoesNotExist()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L

        /** The footer sits straight after the 20 cards of the first page. */
        const val FOOTER_INDEX = 20
    }
}
