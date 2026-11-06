package com.zozi.shoppingapp.data.remote.api

import com.zozi.shoppingapp.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductSearchApi {

    /**
     * A null [query] is omitted from the URL entirely, asking the endpoint to browse the
     * catalogue rather than search it.
     */
    @GET("search")
    suspend fun searchProducts(
        @Query("query") query: String?,
        @Query("page") page: Int,
    ): SearchResponseDto
}
