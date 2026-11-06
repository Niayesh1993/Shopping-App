package com.zozi.shoppingapp.data.repository

import com.zozi.shoppingapp.data.mapper.toDomain
import com.zozi.shoppingapp.data.remote.api.ProductSearchApi
import com.zozi.shoppingapp.data.remote.safeApiCall
import com.zozi.shoppingapp.di.IoDispatcher
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.repository.ProductRepository
import com.zozi.shoppingapp.domain.result.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: ProductSearchApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProductRepository {

    override suspend fun searchProducts(query: String?, page: Int): Outcome<ProductPage> =
        safeApiCall(ioDispatcher) {
            api.searchProducts(query = query, page = page).toDomain()
        }
}
