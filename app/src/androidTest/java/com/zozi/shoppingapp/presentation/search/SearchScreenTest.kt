package com.zozi.shoppingapp.presentation.search

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.testing.product
import com.zozi.shoppingapp.ui.theme.ShoppingAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Drives the stateless [SearchScreen] directly: every branch of the state machine is reachable
 * by constructing the matching [SearchUiState], with no ViewModel or DI involved.
 */
class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setScreen(
        uiState: SearchUiState,
        onQueryChange: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onLoadNextPage: () -> Unit = {},
    ) {
        composeRule.setContent {
            ShoppingAppTheme {
                SearchScreen(
                    uiState = uiState,
                    onQueryChange = onQueryChange,
                    onRetry = onRetry,
                    onLoadNextPage = onLoadNextPage,
                )
            }
        }
    }

    @Test
    fun showsIdlePromptBeforeAnySearch() {
        setScreen(SearchUiState())

        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.search_idle_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertDoesNotExist()
    }

    @Test
    fun showsSpinnerWhileLoading() {
        // The progress indicator animates forever, so the clock would never go idle on its own.
        composeRule.mainClock.autoAdvance = false
        setScreen(SearchUiState(query = "apple", isLoading = true))

        composeRule.onNodeWithTag(SearchTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertDoesNotExist()
        composeRule.onNodeWithTag(SearchTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun showsProductsAndResultCount() {
        setScreen(
            SearchUiState(
                query = "apple",
                products = listOf(
                    product(id = 1, name = "Apple iPhone X 256GB Zilver"),
                    product(id = 2, name = "Apple TV 4K 64GB"),
                ),
                totalResults = 70,
                currentPage = 1,
                hasNextPage = true,
            ),
        )

        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
        composeRule.onAllNodesWithTag(SearchTestTags.PRODUCT_CARD).assertCountEquals(2)
        composeRule.onNodeWithText("Apple iPhone X 256GB Zilver").assertIsDisplayed()
        composeRule.onNodeWithText("Apple TV 4K 64GB").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.RESULT_COUNT).assertIsDisplayed()
        composeRule.onNodeWithText("70 products").assertIsDisplayed()
    }

    @Test
    fun showsEmptyStateForAQueryWithNoMatches() {
        setScreen(SearchUiState(query = "zzzzz"))

        composeRule.onNodeWithTag(SearchTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.search_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertDoesNotExist()
    }

    @Test
    fun emptyStateNamesTheQueryThatFoundNothing() {
        setScreen(SearchUiState(query = "zzzzz"))

        composeRule
            .onNodeWithText(context.getString(R.string.search_empty_subtitle, "zzzzz"))
            .assertIsDisplayed()
    }

    @Test
    fun emptyStateCanClearTheSearch() {
        var cleared: String? = null
        setScreen(SearchUiState(query = "zzzzz"), onQueryChange = { cleared = it })

        composeRule.onNodeWithTag(SearchTestTags.SECONDARY_ACTION).performClick()

        assertEquals("", cleared)
    }

    @Test
    fun showsRetryableErrorWithTitleAndExplanation() {
        setScreen(SearchUiState(query = "apple", error = AppError.NoConnection))

        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.error_no_connection_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.error_no_connection)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).assertIsDisplayed()
    }

    @Test
    fun showsTheServerErrorCodeInTheExplanation() {
        setScreen(SearchUiState(query = "apple", error = AppError.Server(503)))

        composeRule
            .onNodeWithText(context.getString(R.string.error_server, 503))
            .assertIsDisplayed()
    }

    @Test
    fun retryButtonReportsTheRetry() {
        var retries = 0
        setScreen(
            SearchUiState(query = "apple", error = AppError.Timeout),
            onRetry = { retries++ },
        )

        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun hidesRetryForAnErrorThatCannotBeRetried() {
        setScreen(SearchUiState(query = "apple", error = AppError.Unauthorized))

        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.error_unauthorized)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).assertDoesNotExist()
    }

    @Test
    fun keepsProductsVisibleWhenOnlyTheNextPageFailed() {
        setScreen(
            SearchUiState(
                query = "apple",
                products = listOf(product(id = 1)),
                totalResults = 70,
                nextPageError = AppError.Timeout,
            ),
        )

        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertDoesNotExist()
        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.error_next_page)).assertIsDisplayed()
    }

    @Test
    fun nextPageErrorOffersAnInlineRetry() {
        var retries = 0
        setScreen(
            SearchUiState(
                query = "apple",
                products = listOf(product(id = 1)),
                totalResults = 70,
                hasNextPage = true,
                nextPageError = AppError.Timeout,
            ),
            onLoadNextPage = { retries++ },
        )

        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun showsAnInlineSpinnerWhileLoadingTheNextPage() {
        composeRule.mainClock.autoAdvance = false
        setScreen(
            SearchUiState(
                query = "apple",
                products = listOf(product(id = 1)),
                totalResults = 70,
                isLoadingNextPage = true,
                hasNextPage = true,
            ),
        )

        composeRule.onNodeWithTag(SearchTestTags.LOADING_NEXT_PAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
    }

    @Test
    fun typingReportsTheQuery() {
        val typed = mutableListOf<String>()
        setScreen(SearchUiState(), onQueryChange = { typed += it })

        composeRule.onNodeWithTag(SearchTestTags.SEARCH_FIELD).performTextInput("app")

        assertEquals("app", typed.last())
    }

    @Test
    fun clearButtonOnlyAppearsWithAQueryAndClearsIt() {
        var cleared: String? = null
        setScreen(SearchUiState(query = "apple"), onQueryChange = { cleared = it })

        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).performClick()

        assertEquals("", cleared)
    }

    @Test
    fun clearButtonIsHiddenWhileTheQueryIsEmpty() {
        setScreen(SearchUiState())

        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).assertDoesNotExist()
    }
}
