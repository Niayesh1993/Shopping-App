package com.zozi.shoppingapp.testing

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeProductRepository @Inject constructor() : ProductRepository {

    val requests = mutableListOf<Request>()

    /** Lets a test keep a request in flight long enough to assert on the loading state. */
    var responseDelayMillis: Long = 0

    private val responses = mutableMapOf<Int, Outcome<ProductPage>>()
    private var defaultResponse: Outcome<ProductPage> = Outcome.Success(ProductPage.EMPTY)

    data class Request(val query: String, val page: Int)

    fun respondWith(page: ProductPage) {
        defaultResponse = Outcome.Success(page)
    }

    fun respondWith(error: AppError) {
        defaultResponse = Outcome.Failure(error)
    }

    fun respondToPage(page: Int, outcome: Outcome<ProductPage>) {
        responses[page] = outcome
    }

    fun reset() {
        requests.clear()
        responses.clear()
        responseDelayMillis = 0
        defaultResponse = Outcome.Success(ProductPage.EMPTY)
    }

    override suspend fun searchProducts(query: String, page: Int): Outcome<ProductPage> {
        requests += Request(query = query, page = page)
        if (responseDelayMillis > 0) delay(responseDelayMillis)
        return responses[page] ?: defaultResponse
    }
}
