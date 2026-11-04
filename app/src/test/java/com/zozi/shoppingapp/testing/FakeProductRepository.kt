package com.zozi.shoppingapp.testing

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import kotlinx.coroutines.yield

class FakeProductRepository : ProductRepository {

    val requests = mutableListOf<Request>()

    private val responses = mutableMapOf<Int, Outcome<ProductPage>>()
    private var defaultResponse: Outcome<ProductPage> = Outcome.Success(ProductPage.EMPTY)

    data class Request(val query: String, val page: Int)

    fun respondWith(outcome: Outcome<ProductPage>) {
        defaultResponse = outcome
    }

    fun respondWith(page: ProductPage) {
        defaultResponse = Outcome.Success(page)
    }

    fun respondWith(error: AppError) {
        defaultResponse = Outcome.Failure(error)
    }

    fun respondToPage(page: Int, outcome: Outcome<ProductPage>) {
        responses[page] = outcome
    }

    override suspend fun searchProducts(query: String, page: Int): Outcome<ProductPage> {
        requests += Request(query = query, page = page)
        // A real repository always suspends; without it callers never observe their loading state.
        yield()
        return responses[page] ?: defaultResponse
    }
}
