package com.zozi.shoppingapp.domain.usecase

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import com.zozi.shoppingapp.testing.productPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchProductsUseCaseTest {

    private val repository: ProductRepository = mockk()
    private val searchProducts = SearchProductsUseCase(repository)

    /** [ProductRepository] has a single function, so one stub covers every call a test can make. */
    private fun respondWith(outcome: Outcome<ProductPage>) {
        coEvery { repository.searchProducts(any(), any()) } returns outcome
    }

    @Test
    fun `given a blank query when searching then it fails without touching the repository`() = runTest {
        // Given a query that is only whitespace

        // When
        val outcome = searchProducts("   ")

        // Then the network is never asked for something that cannot match
        assertEquals(Outcome.Failure(AppError.EmptyQuery), outcome)
        coVerify(exactly = 0) { repository.searchProducts(any(), any()) }
    }

    @Test
    fun `given a padded query when searching then the repository receives it trimmed`() = runTest {
        // Given
        respondWith(Outcome.Success(productPage()))

        // When
        searchProducts("  apple tv  ")

        // Then
        coVerify(exactly = 1) { repository.searchProducts(query = "apple tv", page = any()) }
    }

    @Test
    fun `given no page when searching then the first page is requested`() = runTest {
        // Given
        respondWith(Outcome.Success(productPage()))

        // When
        searchProducts("apple")

        // Then
        coVerify(exactly = 1) { repository.searchProducts(query = any(), page = 1) }
    }

    @Test
    fun `given a page below the first when searching then it is raised to the first`() = runTest {
        // Given
        respondWith(Outcome.Success(productPage()))

        // When
        searchProducts("apple", page = 0)
        searchProducts("apple", page = -7)

        // Then the API is 1-based, so page 0 and below are not valid requests to forward
        coVerify(exactly = 2) { repository.searchProducts(query = any(), page = 1) }
    }

    @Test
    fun `given the repository succeeds when searching then the page is passed through untouched`() = runTest {
        // Given
        val page = productPage(totalResults = 70)
        respondWith(Outcome.Success(page))

        // When
        val outcome = searchProducts("apple")

        // Then
        assertEquals(Outcome.Success(page), outcome)
    }

    @Test
    fun `given the repository fails when searching then the error is passed through untouched`() = runTest {
        // Given
        respondWith(Outcome.Failure(AppError.NoConnection))

        // When
        val outcome = searchProducts("apple")

        // Then
        assertEquals(Outcome.Failure(AppError.NoConnection), outcome)
    }
}
