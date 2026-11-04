package com.zozi.shoppingapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductTest {

    @Test
    fun `price without a list price has no discount`() {
        val price = Price(salesIncVat = 1279.0)

        assertFalse(price.hasDiscount)
        assertNull(price.discountPercentage)
    }

    @Test
    fun `price with a higher list price is discounted`() {
        val price = Price(salesIncVat = 575.0, listIncVat = 626.0)

        assertTrue(price.hasDiscount)
        assertEquals(8, price.discountPercentage)
    }

    @Test
    fun `price is not discounted when the list price is not higher`() {
        assertFalse(Price(salesIncVat = 575.0, listIncVat = 575.0).hasDiscount)
        assertFalse(Price(salesIncVat = 575.0, listIncVat = 500.0).hasDiscount)
    }

    @Test
    fun `rating without reviews is flagged as empty`() {
        assertFalse(Rating(average = 0.0, count = 0).hasReviews)
        assertTrue(Rating(average = 9.2, count = 209).hasReviews)
    }

    @Test
    fun `page has a next page while the current page trails the page count`() {
        assertTrue(page(currentPage = 1, pageCount = 3).hasNextPage)
        assertTrue(page(currentPage = 2, pageCount = 3).hasNextPage)
        assertFalse(page(currentPage = 3, pageCount = 3).hasNextPage)
    }

    @Test
    fun `empty page reports no further pages`() {
        assertFalse(ProductPage.EMPTY.hasNextPage)
        assertTrue(ProductPage.EMPTY.products.isEmpty())
    }

    private fun page(currentPage: Int, pageCount: Int) = ProductPage(
        products = emptyList(),
        currentPage = currentPage,
        pageCount = pageCount,
        totalResults = 70,
    )
}
