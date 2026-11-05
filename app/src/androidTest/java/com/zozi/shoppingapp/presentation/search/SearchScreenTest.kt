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
 *
 * Test names read as given/when/then in `camelCase` rather than backticks, because `minSdk` is 24
 * and dex rejects method names containing spaces below API 30.
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
    fun givenNothingSearchedYet_whenTheScreenIsShown_thenTheIdlePromptAppears() {
        // Given / When
        setScreen(SearchUiState())

        // Then
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.search_idle_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertDoesNotExist()
    }

    @Test
    fun givenASearchInFlight_whenTheScreenIsShown_thenOnlyTheSpinnerAppears() {
        // Given the progress indicator animates forever, so the clock never goes idle on its own
        composeRule.mainClock.autoAdvance = false

        // When
        setScreen(SearchUiState(query = "apple", isLoading = true))

        // Then
        composeRule.onNodeWithTag(SearchTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertDoesNotExist()
        composeRule.onNodeWithTag(SearchTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun givenResults_whenTheScreenIsShown_thenTheProductsAndTheTotalCountAppear() {
        // Given / When
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

        // Then the count is the total from the API, not the number of cards loaded so far
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
        composeRule.onAllNodesWithTag(SearchTestTags.PRODUCT_CARD).assertCountEquals(2)
        composeRule.onNodeWithText("Apple iPhone X 256GB Zilver").assertIsDisplayed()
        composeRule.onNodeWithText("Apple TV 4K 64GB").assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.RESULT_COUNT).assertIsDisplayed()
        composeRule.onNodeWithText("70 products").assertIsDisplayed()
    }

    @Test
    fun givenAQueryWithNoMatches_whenTheScreenIsShown_thenTheEmptyStateAppears() {
        // Given / When
        setScreen(SearchUiState(query = "zzzzz"))

        // Then
        composeRule.onNodeWithTag(SearchTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.search_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.IDLE).assertDoesNotExist()
    }

    @Test
    fun givenAQueryWithNoMatches_whenTheScreenIsShown_thenTheEmptyStateNamesTheQuery() {
        // Given / When
        setScreen(SearchUiState(query = "zzzzz"))

        // Then
        composeRule
            .onNodeWithText(context.getString(R.string.search_empty_subtitle, "zzzzz"))
            .assertIsDisplayed()
    }

    @Test
    fun givenTheEmptyState_whenItsClearActionIsTapped_thenTheQueryIsCleared() {
        // Given
        var cleared: String? = null
        setScreen(SearchUiState(query = "zzzzz"), onQueryChange = { cleared = it })

        // When
        composeRule.onNodeWithTag(SearchTestTags.SECONDARY_ACTION).performClick()

        // Then
        assertEquals("", cleared)
    }

    @Test
    fun givenARetryableError_whenTheScreenIsShown_thenTitleExplanationAndRetryAppear() {
        // Given / When
        setScreen(SearchUiState(query = "apple", error = AppError.NoConnection))

        // Then
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.error_no_connection_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.error_no_connection)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).assertIsDisplayed()
    }

    @Test
    fun givenAServerError_whenTheScreenIsShown_thenTheStatusCodeIsPartOfTheExplanation() {
        // Given / When
        setScreen(SearchUiState(query = "apple", error = AppError.Server(503)))

        // Then
        composeRule
            .onNodeWithText(context.getString(R.string.error_server, 503))
            .assertIsDisplayed()
    }

    @Test
    fun givenAnError_whenRetryIsTapped_thenTheRetryIsReportedOnce() {
        // Given
        var retries = 0
        setScreen(
            SearchUiState(query = "apple", error = AppError.Timeout),
            onRetry = { retries++ },
        )

        // When
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).performClick()

        // Then
        assertEquals(1, retries)
    }

    @Test
    fun givenAnErrorThatCannotBeRetried_whenTheScreenIsShown_thenNoRetryIsOffered() {
        // Given / When
        setScreen(SearchUiState(query = "apple", error = AppError.Unauthorized))

        // Then a button that cannot help is worse than no button
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.error_unauthorized)).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR_RETRY).assertDoesNotExist()
    }

    @Test
    fun givenOnlyTheNextPageFailed_whenTheScreenIsShown_thenTheResultsStayAndTheErrorIsInline() {
        // Given / When
        setScreen(
            SearchUiState(
                query = "apple",
                products = listOf(product(id = 1)),
                totalResults = 70,
                nextPageError = AppError.Timeout,
            ),
        )

        // Then
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.ERROR).assertDoesNotExist()
        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.error_next_page)).assertIsDisplayed()
    }

    @Test
    fun givenANextPageError_whenItsInlineRetryIsTapped_thenPaginationIsRequestedAgain() {
        // Given
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

        // When
        composeRule.onNodeWithTag(SearchTestTags.NEXT_PAGE_RETRY).performClick()

        // Then
        assertEquals(1, retries)
    }

    @Test
    fun givenTheNextPageIsLoading_whenTheScreenIsShown_thenAnInlineSpinnerJoinsTheResults() {
        // Given
        composeRule.mainClock.autoAdvance = false

        // When
        setScreen(
            SearchUiState(
                query = "apple",
                products = listOf(product(id = 1)),
                totalResults = 70,
                isLoadingNextPage = true,
                hasNextPage = true,
            ),
        )

        // Then the existing results are not replaced by a full screen spinner
        composeRule.onNodeWithTag(SearchTestTags.LOADING_NEXT_PAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.PRODUCT_LIST).assertIsDisplayed()
    }

    @Test
    fun givenTheSearchField_whenTextIsTyped_thenEachChangeIsReported() {
        // Given
        val typed = mutableListOf<String>()
        setScreen(SearchUiState(), onQueryChange = { typed += it })

        // When
        composeRule.onNodeWithTag(SearchTestTags.SEARCH_FIELD).performTextInput("app")

        // Then
        assertEquals("app", typed.last())
    }

    @Test
    fun givenAQuery_whenTheClearButtonIsTapped_thenTheQueryIsCleared() {
        // Given
        var cleared: String? = null
        setScreen(SearchUiState(query = "apple"), onQueryChange = { cleared = it })

        // When
        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).performClick()

        // Then
        assertEquals("", cleared)
    }

    @Test
    fun givenAnEmptyQuery_whenTheScreenIsShown_thenThereIsNoClearButton() {
        // Given / When
        setScreen(SearchUiState())

        // Then
        composeRule.onNodeWithTag(SearchTestTags.CLEAR_QUERY).assertDoesNotExist()
    }
}
