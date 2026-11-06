package com.zozi.shoppingapp.domain.usecase

import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import javax.inject.Inject

class SearchProductsUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    /**
     * A blank [query] is not an error: it asks for the catalogue as-is, which is what the screen
     * shows before the user has typed anything. Surrounding whitespace never reaches the network.
     */
    suspend operator fun invoke(
        query: String,
        page: Int = ProductPage.FIRST_PAGE,
    ): Outcome<ProductPage> = repository.searchProducts(
        query = query.trim().ifBlank { null },
        page = page.coerceAtLeast(ProductPage.FIRST_PAGE),
    )
}
