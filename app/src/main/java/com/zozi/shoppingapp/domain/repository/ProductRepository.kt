package com.zozi.shoppingapp.domain.repository

import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.result.Outcome

interface ProductRepository {
    suspend fun searchProducts(query: String, page: Int): Outcome<ProductPage>
}
