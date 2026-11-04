package com.zozi.shoppingapp.domain.model

data class ProductPage(
    val products: List<Product>,
    val currentPage: Int,
    val pageCount: Int,
    val totalResults: Int,
) {
    val hasNextPage: Boolean
        get() = currentPage < pageCount

    companion object {
        const val FIRST_PAGE = 1

        val EMPTY = ProductPage(
            products = emptyList(),
            currentPage = FIRST_PAGE,
            pageCount = 0,
            totalResults = 0,
        )
    }
}
