package com.zozi.shoppingapp.presentation.search

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
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

    private fun respondWith(outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), any()) } returns outcome
    }

    private fun respondWith(page: ProductPage) = respondWith(Outcome.Success(page))

    private fun respondWith(error: AppError) = respondWith(Outcome.Failure(error))

    private fun respondToPage(page: Int, outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), page) } returns outcome
    }

    /** Leaves the launch catalogue stub in place, so clearing the field can still fall back to it. */
    private fun respondToQuery(query: String, page: ProductPage) {
        coEvery { repository.searchProducts(query = query, page = any()) } returns Outcome.Success(page)
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

    private fun awaitRequestFor(page: Int, times: Int = 1) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            runCatching {
                coVerify(atLeast = times) { repository.searchProducts(any(), page) }
            }.isSuccess
        }
    }

    @Test
    fun givenAFreshLaunch_whenNothingIsTyped_thenTheCatalogueIsAlreadyOnScreen() {
        // Given / When the app has just started

        // Then there is something to scroll before the user has searched for anything
        awaitProductList()
        composeRule.onNodeWithText("Sony WH-1000XM5 Zwart").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertDoesNotExist()
        coVerify(exactly = 1) { repository.searchProducts(query = null, page = 1) }
    }

    @Test
    fun givenTheRepositoryReturnsOnePage_whenAQueryIsTyped_thenTheMatchingProductsAppear() {
        // Given
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
        // Given
        respondWith(AppError.NoConnection)
        typeQuery("apple")
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.ERROR_RETRY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.error_no_connection)).assertIsDisplayed()

        // When
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

        // When
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(19)

        // Then
        awaitRequestFor(page = 2)
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(39)
        composeRule.onNodeWithText("Product 40").assertIsDisplayed()
    }

    @Test
    fun givenTheNextPageFails_whenTheInlineRetryIsTapped_thenTheResultsSurviveAndTheRestArrive() {
        // Given
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

        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(19)
        awaitRequestFor(page = 2)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).performScrollToIndex(FOOTER_INDEX)
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SearchTestTags.NEXT_PAGE_RETRY)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertDoesNotExist()

        // When
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
    fun givenResultsOnScreen_whenTheQueryIsCleared_thenTheCatalogueReturns() {
        // Given
        respondToQuery("apple", productPage(products = listOf(product(id = 1, name = "Apple Watch"))))

        typeQuery("apple")
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Apple Watch").fetchSemanticsNodes().isNotEmpty()
        }

        // When
        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).performClick()

        // Then the user is handed back the catalogue instead of an empty screen
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Sony WH-1000XM5 Zwart").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertDoesNotExist()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L

        const val FOOTER_INDEX = 20
    }
}
