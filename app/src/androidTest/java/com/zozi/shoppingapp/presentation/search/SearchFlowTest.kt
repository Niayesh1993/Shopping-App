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
import com.zozi.shoppingapp.domain.result.Outcome
import com.zozi.shoppingapp.testing.FakeProductRepository
import com.zozi.shoppingapp.testing.product
import com.zozi.shoppingapp.testing.productPage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * End-to-end through the real ViewModel, use case and DI graph, with only the network-backed
 * repository swapped out. Covers debounce, pagination and error recovery as the user sees them.
 */
@HiltAndroidTest
class SearchFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: FakeProductRepository

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
        repository.reset()
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

    @Test
    fun startsOnTheIdlePrompt() {
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertIsDisplayed()
        assertEquals(emptyList<FakeProductRepository.Request>(), repository.requests)
    }

    @Test
    fun searchingShowsTheMatchingProducts() {
        // A single page, so the list does not immediately prefetch a second one.
        repository.respondWith(
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

        typeQuery("apple")
        awaitProductList()

        composeRule.onAllNodesWithTag(SearchTestTags.PRODUCT_CARD).assertCountEquals(2)
        composeRule.onNodeWithText("Apple iPhone X 256GB Zilver").assertIsDisplayed()
        composeRule.onNodeWithText("2 products").assertIsDisplayed()
        assertEquals("apple", repository.requests.single().query)
    }

    @Test
    fun showsTheEmptyStateWhenNothingMatches() {
        repository.respondWith(productPage(products = emptyList(), pageCount = 0, totalResults = 0))

        typeQuery("zzzzz")

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.EMPTY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.search_empty_title)).assertIsDisplayed()
    }

    @Test
    fun retryRecoversFromAFailedSearch() {
        repository.respondWith(AppError.NoConnection)

        typeQuery("apple")

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.ERROR_RETRY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.error_no_connection)).assertIsDisplayed()

        repository.respondWith(productPage(products = listOf(product(id = 7, name = "Apple Watch"))))
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).performClick()
        awaitProductList()

        composeRule.onNodeWithText("Apple Watch").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertDoesNotExist()
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun scrollingToTheEndLoadsTheNextPage() {
        repository.respondToPage(
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
        repository.respondToPage(
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

        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(19)

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            repository.requests.any { it.page == 2 }
        }

        // The appended items exist but are off-screen: only visible rows are composed.
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(39)
        composeRule.onNodeWithText("Product 40").assertIsDisplayed()
    }

    @Test
    fun aFailedNextPageKeepsTheResultsAndRecoversOnRetry() {
        val firstPage = productPage(
            products = (1..20L).map { product(id = it, name = "Product $it") },
            currentPage = 1,
            pageCount = 2,
            totalResults = 40,
        )
        repository.respondToPage(1, Outcome.Success(firstPage))
        repository.respondToPage(2, Outcome.Failure(AppError.Timeout))

        typeQuery("product")
        awaitProductList()

        // Only a handful of cards fit on screen, so the next page is requested on reaching the end.
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(19)
        composeRule.waitUntil(TIMEOUT_MILLIS) { repository.requests.any { it.page == 2 } }
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

        repository.respondToPage(
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

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            repository.requests.count { it.page == 2 } == 2
        }
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(39)
        composeRule.onNodeWithText("Product 40").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_ERROR).assertDoesNotExist()
    }

    @Test
    fun clearingTheQueryReturnsToTheIdlePrompt() {
        repository.respondWith(productPage(products = listOf(product(id = 1))))

        typeQuery("apple")
        awaitProductList()

        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).performClick()

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
