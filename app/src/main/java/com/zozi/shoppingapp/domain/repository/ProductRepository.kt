package com.zozi.shoppingapp.domain.repository

import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.result.Outcome

interface ProductRepository {

    /** A null [query] browses the whole catalogue instead of searching within it. */
    suspend fun searchProducts(query: String?, page: Int): Outcome<ProductPage>
}
