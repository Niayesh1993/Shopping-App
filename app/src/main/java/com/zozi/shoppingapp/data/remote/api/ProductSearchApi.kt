package com.zozi.shoppingapp.data.remote.api

import com.zozi.shoppingapp.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductSearchApi {
    @GET("search")
    suspend fun searchProducts(
        @Query("query") query: String,
        @Query("page") page: Int,
    ): SearchResponseDto
}
