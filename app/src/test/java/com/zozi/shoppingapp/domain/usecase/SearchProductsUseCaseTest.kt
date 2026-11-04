package com.zozi.shoppingapp.domain.usecase

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.result.Outcome
import com.zozi.shoppingapp.testing.FakeProductRepository
import com.zozi.shoppingapp.testing.productPage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchProductsUseCaseTest {

    private val repository = FakeProductRepository()
    private val searchProducts = SearchProductsUseCase(repository)

    @Test
    fun `rejects a blank query without touching the repository`() = runTest {
        val outcome = searchProducts("   ")

        assertEquals(Outcome.Failure(AppError.EmptyQuery), outcome)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun `trims the query before searching`() = runTest {
        repository.respondWith(productPage())

        searchProducts("  apple tv  ")

        assertEquals("apple tv", repository.requests.single().query)
    }

    @Test
    fun `defaults to the first page`() = runTest {
        repository.respondWith(productPage())

        searchProducts("apple")

        assertEquals(1, repository.requests.single().page)
    }

    @Test
    fun `never asks for a page below the first`() = runTest {
        repository.respondWith(productPage())

        searchProducts("apple", page = 0)
        searchProducts("apple", page = -7)

        assertEquals(listOf(1, 1), repository.requests.map { it.page })
    }

    @Test
    fun `passes through the repository outcome`() = runTest {
        val page = productPage(totalResults = 70)
        repository.respondWith(page)

        assertEquals(Outcome.Success(page), searchProducts("apple"))
    }

    @Test
    fun `passes through repository failures`() = runTest {
        repository.respondWith(AppError.NoConnection)

        assertEquals(Outcome.Failure(AppError.NoConnection), searchProducts("apple"))
    }
}
