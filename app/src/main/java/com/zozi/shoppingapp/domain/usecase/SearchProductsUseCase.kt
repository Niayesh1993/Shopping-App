package com.zozi.shoppingapp.domain.usecase

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import javax.inject.Inject

class SearchProductsUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(
        query: String,
        page: Int = ProductPage.FIRST_PAGE,
    ): Outcome<ProductPage> {
        val normalisedQuery = query.trim()
        if (normalisedQuery.isEmpty()) {
            return Outcome.Failure(AppError.EmptyQuery)
        }
        return repository.searchProducts(
            query = normalisedQuery,
            page = page.coerceAtLeast(ProductPage.FIRST_PAGE),
        )
    }
}
